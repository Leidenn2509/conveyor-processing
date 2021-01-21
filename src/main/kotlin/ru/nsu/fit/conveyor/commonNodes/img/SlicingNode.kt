package ru.nsu.fit.conveyor.commonNodes.img

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import ru.nsu.fit.conveyor.node.NewBaseNode
import ru.nsu.fit.conveyor.node.NodeInput
import ru.nsu.fit.conveyor.node.NodeOutput

@ExperimentalCoroutinesApi
class SlicingNode : NewBaseNode("Slicing into areas") {
    init {
        inputs[0] = NodeInput(this, 0, Image::class)
        inputs[1] = NodeInput(this, 1, Int::class)
        outputs[0] = NodeOutput(this, 0, Image::class)
    }

    @Suppress("UNCHECKED_CAST")
    private val inputImage: Channel<Image>
        get() = inputs[0]!!.channel as Channel<Image>

    @Suppress("UNCHECKED_CAST")
    private val inputN: Channel<Int>
        get() = inputs[1]!!.channel as Channel<Int>

    @Suppress("UNCHECKED_CAST")
    private val output: Channel<Image>
        get() = outputs[0]!!.channel as Channel<Image>


    var n: Int? = null

    override val isReady: Boolean
        get() {
            return if (n == null) {
                !inputN.isEmpty && !inputImage.isEmpty
            } else {
                !inputImage.isEmpty
            }
        }

    override suspend fun body() {
        if (n == null) {
            n = inputN.receive()
        } else if (n != null && !inputN.isEmpty) {
            n = inputN.receive()
        }
        if (n!! <= 0) return

        val image = inputImage.receive()
        val width = image.width / n!!
        for (i in 0 until n!!) {
            output.send(
                Image(
                    width,
                    image.height,
                    image.type,
                    mutableListOf(Slicing(i, n!!, image))
                )
            )
        }

    }
}