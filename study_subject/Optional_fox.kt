fun main() {
    try {
        val opt: Optional<Int> = Optional.of(null)

        val orElse: Int = opt.map { it + 1 }
            .map { it + 2 }.orElse(342)
        println(orElse)

        val get = Optional.of(3)
            .filter { it > 3 }
            .get()
        println(get)

        val get2 = Optional.of(3)
            .map { it.toString()  }
            .map { it == "3"}
            .get()
        println(get2)

        val orElse1 = Optional.of(listOf(1, 2, 3, 4))
            .map { it.size }
            .orElse(5)
        println(orElse1)

        listOf(1, 2, 3).map {
            it + 3
        }.stream().map { it }

        val fnOp = Optional.of(
            { a:Int -> a + 100 }
        )

        val dOp = Optional.of(50)

        val rOp = applyi(fnOp, dOp)

        println(rOp.get())

    } catch(e: Exception) {
        e.printStackTrace()
    }
}
