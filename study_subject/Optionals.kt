package com.foxrain.sheep.whileblack.util.study_subject

/**
 * 한글 번역 null이 아닌 값을 포함하거나 포함하지 않을 수 있는 컨테이너 개체입니다. 값이 있으면 isPresent()는
 * true를 반환합니다. 값이 없으면 객체가 비어 있는 것으로 간주되고 isPresent()는 false를 반환합니다.
 * orElse()(값이 없으면 기본값 반환) 및 ifPresent()(값이 있으면 작업 수행)과 같이 포함된 값의 유무에 따라
 * 달라지는 추가 메서드가 제공됩니다.
 *
 * 이것은 값 기반 클래스입니다. 선택적 인스턴스에서 ID에 민감한 작업(참조 같음(==), ID 해시 코드 또는 동기화 포함)을
 * 사용하면 예측할 수 없는 결과가 발생할 수 있으므로 피해야 합니다.
 *
 * API 참고: 옵셔널은 주로 "결과 없음"을 명시해야 하고 null을 사용하면 오류가 발생할 가능성이 있는 메서드 반환 유형으로
 * 사용하기 위한 것입니다. 유형이 선택적인 변수는 그 자체가 널이 아니어야 합니다. 항상 Optional 인스턴스를 가리켜야
 * 합니다. 부터: 1.8
 */
class Optionals<T>
// private 생성자 관련해서는 이런 방식으로 작성한다.
private constructor(private var value: T) {
    companion object {
        private val EMPTY = Optionals(null)

        /**
         * Generic 함수 정의 Generic 함수를 정의할 때, 타입이 정해지지 않은 변수는 함수 이름 앞에 <T>처럼 정의되어야
         * 합니다. 아래 코드는 타입 T 변수 num1과 num2를 더하고 타입 T 변수를 리턴하는 함수입니다.
         */
        fun <K> of(t: K) = Optionals(t)

        fun <T> ofNullable(t: T): Optionals<out T?> {
            return if (t != null) {
                Optionals(t)
            } else {
                EMPTY
            }
        }
    }


    fun isPresent() = value != null

    fun ifPresent(action: (T) -> Unit) {
        if (value != null) action(value)
    }

    fun isEmpty() = value == null

    fun get(): T {
        if (value == null) {
            throw RuntimeException("No value present")
        }
        return value
    }

    fun ifPresentOrElse(action: (T) -> Unit, el: () -> Unit) {
        if (value != null) {
            action(value)
        } else {
            el()
        }
    }

    fun filter(predicate: (T) -> Boolean): Optionals<T>? {
        return if (predicate(value)) {
            Optionals(value)
        } else {
            null
        }
    }

    fun <U> map(mapper:(T) -> U): Optionals<out U?> {
        return if (!isPresent()) {
            return EMPTY
        } else {
            ofNullable(mapper(value))
        }
    }

    fun <U> flatMap(mapper:(T) -> Optionals<U>): Optionals<out U?> {
        return if (!isPresent()) {
            EMPTY
        } else {
            mapper(value)
        }
    }

    fun orElse(other:T): T {
        return if(value != null) {
            return value
        } else {
            return other
        }
    }

    fun orElseGet(new:() -> T): T {
        return if(value != null) {
            return value
        } else {
            new()
        }
    }
}

fun main() {
    val optionals = Optionals.of("a")
    val get = optionals.filter { it.equals("a") }!!.get()
    System.out.println(get)
}
