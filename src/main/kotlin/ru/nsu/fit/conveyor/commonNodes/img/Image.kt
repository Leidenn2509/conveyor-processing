package ru.nsu.fit.conveyor.commonNodes.img

data class Image(
    var width: Int,
    var height: Int,
    var type: Type,
    val operations: MutableList<ImageOperation> = mutableListOf<ImageOperation>()
) {
    enum class Type {
        PNG,
        JPG
    }
}
