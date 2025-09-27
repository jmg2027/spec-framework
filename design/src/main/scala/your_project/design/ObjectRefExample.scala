package your_project.design

import framework.spec.Spec._
import your_project.specs.MyExampleSpecs._

/** Example demonstrating that DSL methods `is`, `has`, and `uses`
  * accept HardwareSpecification objects.
  */
object ObjectRefExample {
  val exampleSpec =
    CONTRACT("ObjRefExample").desc("Spec with object references")
      .is(TestInterfaceSpec, DummyStatusSpec)
      .has(DummyObjSpec)
      .uses(TestModuleSpec)
      .build()
}
