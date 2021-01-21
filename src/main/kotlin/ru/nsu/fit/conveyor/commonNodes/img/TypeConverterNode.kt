package ru.nsu.fit.conveyor.commonNodes.img

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import ru.nsu.fit.conveyor.node.BaseNode
import ru.nsu.fit.conveyor.node.NodeInput
import ru.nsu.fit.conveyor.node.NodeOutput

@ExperimentalCoroutinesApi
@Suppress("LeakingThis")
open class TypeConverterNode(
    val toType: Image.Type,
    description: String = "Change type to $toType"
) : BaseNode(description) {

    private inner class TypeConverterNodeContext: Context() {
        init {
            inputs[0] = NodeInput(this@TypeConverterNode, 0, Image::class)
            outputs[0] = NodeOutput(this@TypeConverterNode, 0, Image::class)
        }

        @Suppress("UNCHECKED_CAST")
        val inputImg: Channel<Image>
            get() = inputs[0]!!.channel as Channel<Image>

        @Suppress("UNCHECKED_CAST")
        val outputImg: Channel<Image>
            get() = outputs[0]!!.channel as Channel<Image>
    }

    override fun initContext(withContext: Any?) {
        contexts[withContext] = TypeConverterNodeContext()
    }

    init {
        initContext(null)
    }

    override suspend fun body(context: Context) = with(context as TypeConverterNodeContext) {
        log("Try receive image", context)
        val image = inputImg.receive()
        log("Receive image: $image", context)
        val imageType = image.type
        if (imageType != toType) {
            log("Do work", context)
            delay(DELAY)
            image.operations.add(ChangeType(image.type, toType))
            image.type = toType
            log("New image: $image", context)

        } else {
            log("Skip. Same type", context)
        }
        log("Send new image to outputs", context)
        outputImg.send(image)
        log("End of work", context)
    }

    companion object {
        const val DELAY = 1000L
    }
}

@ExperimentalCoroutinesApi
class ChangeTypeToJPG() : TypeConverterNode(Image.Type.JPG)

@ExperimentalCoroutinesApi
class ChangeTypeToPNG() : TypeConverterNode(Image.Type.PNG)
