package info.laht.threekt.extras.objects

import info.laht.threekt.materials.MaterialProxy
import info.laht.threekt.core.Object3D
import info.laht.threekt.materials.Material

class ImmediateRenderObject(
    override var material: Material
) : Object3D(), MaterialProxy {

    var render: () -> Unit = {}

}
