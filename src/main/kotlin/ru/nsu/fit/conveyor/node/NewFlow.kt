package ru.nsu.fit.conveyor.node

import kotlinx.coroutines.ExperimentalCoroutinesApi
import ru.nsu.fit.conveyor.commonNodes.img.Image

@ExperimentalCoroutinesApi
class NewFlow(description: String) : NewBaseNode(description) {
    private val nodes: MutableList<NewBaseNode> = mutableListOf()


//    fun addNode(id: Int, node: NewBaseNode, connections: NodeConnector.() -> Unit = {}) {
//        nodes.add(id, node)
//        NodeConnector(this, node).connections()
//    }

    override suspend fun body() {
        1 + 1
    }

}