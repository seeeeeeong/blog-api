package com.blog.api.core.support.nickname

import org.springframework.stereotype.Component

@Component
class RandomNicknameGenerator {

    fun generate(): String {
        val adjective = ADJECTIVES.random()
        val animal = ANIMALS.random()
        return "$adjective $animal"
    }

    companion object {
        private val ADJECTIVES = listOf(
            "행복한", "용감한", "조용한", "빛나는", "따뜻한",
            "씩씩한", "지혜로운", "상냥한", "활발한", "차분한",
            "재빠른", "느긋한", "명랑한", "신비로운", "귀여운",
            "부지런한", "담대한", "청량한", "포근한", "영리한",
            "소중한", "유쾌한", "든든한", "산뜻한", "호기심많은",
        )

        private val ANIMALS = listOf(
            "고양이", "강아지", "토끼", "여우", "사자",
            "펭귄", "코알라", "판다", "부엉이", "돌고래",
            "다람쥐", "수달", "하마", "기린", "나비",
            "참새", "거북이", "고슴도치", "햄스터", "알파카",
            "치타", "플라밍고", "카멜레온", "해달", "레서판다",
        )
    }
}
