package ru.nsu.fit.conveyor.commonNodes.img

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import ru.nsu.fit.conveyor.node.BaseNode
import ru.nsu.fit.conveyor.node.NodeInput
import ru.nsu.fit.conveyor.node.NodeOutput

@ExperimentalCoroutinesApi
class PanoramaNode : BaseNode("Panorama") {
    private inner class PanoramaNodeContext : Context() {
        @Suppress("UNCHECKED_CAST")
        val inputImages: Channel<Image>
            get() = inputs[0]!!.channel as Channel<Image>

        @Suppress("UNCHECKED_CAST")
        val inputN: Channel<Int>
            get() = inputs[1]!!.channel as Channel<Int>

        @Suppress("UNCHECKED_CAST")
        val output: Channel<Image>
            get() = outputs[0]!!.channel as Channel<Image>

        var n: Int? = null

        init {
            inputs[0] = NodeInput(this@PanoramaNode, 0, Image::class)
            inputs[1] = NodeInput(this@PanoramaNode, 1, Int::class)
            outputs[0] = NodeOutput(this@PanoramaNode, 0, Image::class)
        }
    }

    override fun initContext(withContext: Any?) {
        contexts[withContext] = PanoramaNodeContext()
    }

    override fun isReady(withContext: Any?): Boolean = with(contexts[withContext]!! as PanoramaNodeContext) {
        return if (n == null) {
            !inputN.isEmpty && !inputImages.isEmpty
        } else {
            !inputImages.isEmpty
        }
    }

    init {
        initContext(null)
    }

    override suspend fun body(context: Context) = with(context as PanoramaNodeContext) {
        if (n == null) {
            n = inputN.receive()
        } else if (n != null && !inputN.isEmpty) {
            n = inputN.receive()
        }
        if (n!! <= 0) return

        val images = mutableListOf<Image>()
        repeat(n!!) {
            images.add(inputImages.receive())
        }
        val panorama = Image(
            images.sumBy { it.width },
            images.maxByOrNull { it.height }!!.height,
            Image.Type.PNG,
            mutableListOf(Panorama(images))
        )
        output.send(panorama)
    }
}