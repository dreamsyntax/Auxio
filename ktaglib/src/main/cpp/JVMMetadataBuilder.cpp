//
// Created by oxycblt on 12/12/24.
//

#include "JVMMetadataBuilder.h"

#include <taglib/mp4tag.h>
#include <taglib/textidentificationframe.h>

JVMMetadataBuilder::JVMMetadataBuilder(JNIEnv *env) : env(env), id3v2(env), xiph(env), mp4(env),
                                                      cover(), properties(nullptr) {}

void JVMMetadataBuilder::setMimeType(const std::string_view mimeType) {
    this->mimeType = mimeType;
}

void JVMMetadataBuilder::setId3v2(const TagLib::ID3v2::Tag &tag) {
    for (auto frame: tag.frameList()) {
        if (auto txxxFrame = dynamic_cast<TagLib::ID3v2::UserTextIdentificationFrame *>(frame)) {
            TagLib::String desc = std::string(txxxFrame->description().toCString(true));
            // Make desc lowercase
            std::transform(desc.begin(), desc.end(), desc.begin(), ::tolower);
            TagLib::String key = TagLib::String(frame->frameID()) + ":" + desc;
            TagLib::StringList frameText = txxxFrame->fieldList();
            frameText.erase(frameText.begin()); // Remove description that exists for some insane reason
            id3v2.add(key, frameText);
        } else if (auto textFrame = dynamic_cast<TagLib::ID3v2::TextIdentificationFrame *>(frame)) {
            TagLib::String key = frame->frameID();
            TagLib::StringList frameText = textFrame->fieldList();
            id3v2.add(key, frameText);
        } else {
            continue;
        }
    }
}

void JVMMetadataBuilder::setXiph(const TagLib::Ogg::XiphComment &tag) {
    for (auto field: tag.fieldListMap()) {
        auto fieldName = std::string(field.first.toCString(true));
        std::transform(fieldName.begin(), fieldName.end(), fieldName.begin(), ::tolower);
        auto taglibFieldName = TagLib::String(fieldName);
        auto fieldValue = field.second;
        xiph.add(taglibFieldName, fieldValue);
    }
}

void JVMMetadataBuilder::setMp4(const TagLib::MP4::Tag &tag) {
    for (auto item: tag.itemMap()) {
        auto itemName = TagLib::String(item.first);
        auto itemValue = item.second;
        auto type = itemValue.type();

        // Only read out the atoms for the reasonable tags we are expecting.
        // None of the crazy binary atoms.
        if (type == TagLib::MP4::Item::Type::StringList) {
            auto value = itemValue.toStringList();
            mp4.add(itemName, value);
            return;
        }

        // Assume that taggers will be unhinged and store track numbers
        // as ints, uints, or longs.
        if (type == TagLib::MP4::Item::Type::Int) {
            auto value = std::to_string(itemValue.toInt());
            id3v2.add(itemName, value);
            return;
        }
        if (type == TagLib::MP4::Item::Type::UInt) {
            auto value = std::to_string(itemValue.toUInt());
            id3v2.add(itemName, value);
            return;
        }
        if (type == TagLib::MP4::Item::Type::LongLong) {
            auto value = std::to_string(itemValue.toLongLong());
            id3v2.add(itemName, value);
            return;
        }
        if (type == TagLib::MP4::Item::Type::IntPair) {
            // It's inefficient going from the integer representation back into
            // a string, but I fully expect taggers to just write "NN/TT" strings
            // anyway, and musikr doesn't have to do as much fiddly variant handling.
            auto value = std::to_string(itemValue.toIntPair().first) + "/" +
                         std::to_string(itemValue.toIntPair().second);
            id3v2.add(itemName, value);
            return;
        }
        // Nothing else makes sense to handle as far as I can tell.
    }
}

void JVMMetadataBuilder::setCover(const TagLib::List<TagLib::VariantMap> covers) {
    if (covers.isEmpty()) {
        return;
    }
    // Find the cover with a "front cover" type
    for (auto cover: covers) {
        auto type = cover["pictureType"].toString();
        if (type == "Front Cover") {
            this->cover = cover["data"].toByteVector();
            return;
        }
    }
    // No front cover, just pick first.
    // TODO: Consider having cascading fallbacks to increasingly less
    //  relevant covers perhaps
    this->cover = covers.front()["data"].toByteVector();
}

void JVMMetadataBuilder::setProperties(TagLib::AudioProperties *properties) {
    this->properties = properties;
}

jobject JVMMetadataBuilder::build() {
    jclass propertiesClass = env->FindClass("org/oxycblt/ktaglib/Properties");
    jmethodID propertiesInit = env->GetMethodID(propertiesClass, "<init>", "(Ljava/lang/String;JII)V");
    jobject propertiesObj = env->NewObject(propertiesClass, propertiesInit,
                                           env->NewStringUTF(mimeType.data()), (jlong) properties->lengthInMilliseconds(),
                                           properties->bitrate(), properties->sampleRate());
    env->DeleteLocalRef(propertiesClass);

    jclass metadataClass = env->FindClass("org/oxycblt/ktaglib/Metadata");
    jmethodID metadataInit = env->GetMethodID(metadataClass, "<init>", "(Ljava/util/Map;Ljava/util/Map;Ljava/util/Map;[BLorg/oxycblt/ktaglib/Properties;)V");
    jobject id3v2Map = id3v2.getObject();
    jobject xiphMap = xiph.getObject();
    jobject mp4Map = mp4.getObject();
    jbyteArray coverArray = nullptr;
    if (cover.has_value()) {
        coverArray = env->NewByteArray(cover->size());
        env->SetByteArrayRegion(coverArray, 0, cover->size(), reinterpret_cast<const jbyte *>(cover->data()));
    }
    jobject metadataObj = env->NewObject(metadataClass, metadataInit, id3v2Map, xiphMap, mp4Map, coverArray, propertiesObj);
    env->DeleteLocalRef(metadataClass);
    return metadataObj;
}