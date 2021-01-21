//package ru.nsu.fit.conveyor.node
//
//class TestUtils {
//    companion object {
//        fun identityNode(clazz: Class<*> = Any::class.java): Node  {
//            return Node("identity").apply {
//                addInput(0, clazz)
//                addOutput(0, clazz)
//                body = {
//                    it
//                }
//            }
//        }
//    }
//}