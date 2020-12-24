package ru.nsu.fit.conveyor.node

import java.util.*

class Node(
    description: String = "",
    var flow: Flow? = null
) : BaseNode(description) {
    var body: (DataById) -> DataById = { mapOf() }

    override fun copy(): Node {
        return Node(description).apply {
            this@Node.inputTypes.forEach(this::addInput)
            this@Node.outputTypes.forEach(this::addOutput)
            this.body = this@Node.body
        }
    }

    override suspend fun run(inputs: DataById): DataById = body(inputs)
}



