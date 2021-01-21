package ru.nsu.fit.conveyor.commonNodes.img

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import ru.nsu.fit.conveyor.node.NewBaseNode
import ru.nsu.fit.conveyor.node.NodeInput
import ru.nsu.fit.conveyor.node.NodeOutput

@ExperimentalCoroutinesApi
@Suppress("LeakingThis")
open class TypeConverterNode(
    val toType: Image.Type,
    description: String = "Change type to $toType"
) : NewBaseNode(description) {

    init {
        inputs[0] = NodeInput(this, 0, Image::class)
        outputs[0] = NodeOutput(this, 0, Image::class)
    }

    @Suppress("UNCHECKED_CAST")
    private val inputImg: Channel<Image>
        get() = inputs[0]!!.channel as Channel<Image>

    @Suppress("UNCHECKED_CAST")
    private val outputImg: Channel<Image>
        get() = outputs[0]!!.channel as Channel<Image>

    override suspend fun body() {
        log("Try receive image")
        val image = inputImg.receive()
        log("Receive image: $image")
        val imageType = image.type
        if (imageType != toType) {
            log("Do work")
            delay(DELAY)
            image.operations.add(ChangeType(image.type, toType))
            image.type = toType
            log("New image: $image")

        } else {
            log("Skip. Same type")
        }
        log("Send new image to outputs")
        outputImg.send(image)
        log("End of work")
    }

    companion object {
        const val DELAY = 1000L
    }
}

@ExperimentalCoroutinesApi
class ChangeTypeToJPG() : TypeConverterNode(Image.Type.JPG)

@ExperimentalCoroutinesApi
class ChangeTypeToPNG() : TypeConverterNode(Image.Type.PNG)
