package ru.nsu.fit.conveyor.commonNodes.img

sealed class ImageOperation

data class Resize(
    val addToWidth: Int,
    val addToHeight: Int
) : ImageOperation()

data class ChangeType(
    val fromType: Image.Type,
    val toType: Image.Type
) : ImageOperation()

data class Panorama(
    val images: List<Image>
) : ImageOperation()

data class Slicing(
    val id: Int,
    val n: Int,
    val original: Image
) : ImageOperation()

sealed class Filter : ImageOperation()

class Gaussian : Filter()