package ru.nsu.fit.conveyor.commonNodes.img

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import ru.nsu.fit.conveyor.node.NewBaseNode
import ru.nsu.fit.conveyor.node.NodeInput
import ru.nsu.fit.conveyor.node.NodeOutput
import kotlin.random.Random

@ExperimentalCoroutinesApi
class ChooseNodeFromN(val n: Int) : NewBaseNode("ChooseNode") {
    init {
        outputs[0] = NodeOutput(this, 0, Image::class)
        for (i in 0 until n) {
            inputs[i] = NodeInput(this, i, Image::class)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private val outputImg: Channel<Image>
        get() = outputs[0]!!.channel as Channel<Image>

    override suspend fun body() {
        val images = mutableListOf<Image>()
        inputs.forEach { (_, input) ->
            @Suppress("UNCHECKED_CAST")
            images.add((input.channel as Channel<Image>).receive())
        }
        delay(DELAY)
        outputImg.send(images[Random.nextInt(n)])
    }

    companion object {
        const val DELAY = 1500L
    }
}