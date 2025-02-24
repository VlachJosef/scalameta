package scala.meta.tests.parsers.dotty

import scala.meta.tests.parsers._
import scala.meta._
import scala.meta.Term.Block

class SignificantIndentationSuite extends BaseDottySuite {

  val defx = Decl.Def(Nil, Term.Name("f"), Nil, Nil, Type.Name("Int"))
  val defy = Defn.Def(
    Nil,
    Term.Name("y"),
    Nil,
    Nil,
    Some(Type.Name("String")),
    Term.Block(List(Term.Apply(Term.Name("fa"), Nil), Term.Apply(Term.Name("fb"), Nil)))
  )

  test("basic-example") {
    val code = """|trait A:
                  |  def f: Int
                  |""".stripMargin
    runTestAssert[Stat](code, assertLayout = Some("trait A { def f: Int }"))(
      Defn.Trait(
        Nil,
        Type.Name("A"),
        Nil,
        Ctor.Primary(Nil, Name(""), Nil),
        Template(Nil, Nil, Self(Name(""), None), List(defx))
      )
    )
  }

  test("multistat-example") {
    val code = """|trait A:
                  |  def f: Int
                  |  def y: String = { fa(); fb() }""".stripMargin
    runTestAssert[Stat](code, assertLayout = None)(
      Defn.Trait(
        Nil,
        Type.Name("A"),
        Nil,
        Ctor.Primary(Nil, Name(""), Nil),
        Template(
          Nil,
          Nil,
          Self(Name(""), None),
          List(
            defx,
            defy
          )
        )
      )
    )
  }

  test("indent-and-back") {
    val code = """|object O:
                  |  class C:
                  |    def f: Int = 3
                  |  trait T:
                  |    def f: Int = 4
                  |""".stripMargin
    runTestAssert[Stat](code, assertLayout = None)(
      Defn.Object(
        Nil,
        Term.Name("O"),
        Template(
          Nil,
          Nil,
          Self(Name(""), None),
          List(
            Defn.Class(
              Nil,
              Type.Name("C"),
              Nil,
              ctor,
              Template(
                Nil,
                Nil,
                Self(Name(""), None),
                List(Defn.Def(Nil, Term.Name("f"), Nil, Nil, Some(Type.Name("Int")), Lit.Int(3)))
              )
            ),
            Defn.Trait(
              Nil,
              Type.Name("T"),
              Nil,
              Ctor.Primary(Nil, Name(""), Nil),
              Template(
                Nil,
                Nil,
                Self(Name(""), None),
                List(Defn.Def(Nil, Term.Name("f"), Nil, Nil, Some(Type.Name("Int")), Lit.Int(4)))
              )
            )
          )
        )
      )
    )
  }

  test("then-no-indent") {
    // this test is related to dotty issue: https://github.com/lampepfl/dotty/issues/9790
    // It should either assert error during parsing: "illegal start of simple expression"
    // Or accept mismatch with parsing rules and parse as 'if (cond) { truep } else {falsep }'
    // Why error is thrown is described in mentioned issue.
    val code = """|def fn: Unit =
                  |    if cond then
                  |  truep
                  |    else
                  |  falsep
                  |    otherStatement()
                  |""".stripMargin
    runTestAssert[Stat](
      code,
      assertLayout = Some(
        """|def fn: Unit = {
           |  if (cond) truep else falsep
           |  otherStatement()
           |}
           |""".stripMargin
      )
    )(
      Defn.Def(
        Nil,
        Term.Name("fn"),
        Nil,
        Nil,
        Some(Type.Name("Unit")),
        Term.Block(
          List(
            Term.If(Term.Name("cond"), Term.Name("truep"), Term.Name("falsep")),
            Term.Apply(Term.Name("otherStatement"), Nil)
          )
        )
      )
    )
  }

  test("then-no-indent-wrong") {
    // this test is related to dotty issue: https://github.com/lampepfl/dotty/issues/9790
    // It should either assert error during parsing: "illegal start of simple expression"
    // Or accept mismatch with parsing rules and parse as 'if (cond) { truep } else {falsep }'
    // Why error is thrown is described in mentioned issue.
    val code = """|def fn: Unit =
                  |    if cond then
                  |  truep1
                  |  truep2
                  |    else
                  |  falsep
                  |""".stripMargin
    runTestError[Stat](code, "expected but else found")
  }

  test("indent-inside-brace-ok") {
    val code = """|class X {
                  |  def fx(): Unit =
                  |    f1
                  |    f2
                  |}
                  |""".stripMargin
    val output = """|class X {
                    |  def fx(): Unit = {
                    |    f1
                    |    f2
                    |  }
                    |}
                    |""".stripMargin
    runTestAssert[Stat](code, assertLayout = Some(output))(
      Defn.Class(
        Nil,
        Type.Name("X"),
        Nil,
        Ctor.Primary(Nil, Name(""), Nil),
        Template(
          Nil,
          Nil,
          Self(Name(""), None),
          List(
            Defn.Def(
              Nil,
              Term.Name("fx"),
              Nil,
              List(List()),
              Some(Type.Name("Unit")),
              Term.Block(List(Term.Name("f1"), Term.Name("f2")))
            )
          )
        )
      )
    )
  }

  test("indent-inside-brace-not") {
    val code = """|class X {
                  |      def fx(): Unit =
                  |    f1
                  |    f2
                  |}
                  |""".stripMargin
    val output = """|class X {
                    |  def fx(): Unit = f1
                    |  f2
                    |}
                    |""".stripMargin
    runTestAssert[Stat](code, assertLayout = Some(output))(
      Defn.Class(
        Nil,
        Type.Name("X"),
        Nil,
        Ctor.Primary(Nil, Name(""), Nil),
        Template(
          Nil,
          Nil,
          Self(Name(""), None),
          List(
            Defn.Def(
              Nil,
              Term.Name("fx"),
              Nil,
              List(List()),
              Some(Type.Name("Unit")),
              Term.Name("f1")
            ),
            Term.Name("f2")
          )
        )
      )
    )
  }

  test("should-indent-yet-brace") {
    val code = """|class X {
                  |  def fx(): Unit =
                  |  {
                  |
                  |  }
                  |  private def f2: Int = 1
                  |}
                  |""".stripMargin
    val output = """|class X {
                    |  def fx(): Unit = {}
                    |  private def f2: Int = 1
                    |}
                    |""".stripMargin
    runTestAssert[Stat](code, assertLayout = Some(output))(
      Defn.Class(
        Nil,
        Type.Name("X"),
        Nil,
        Ctor.Primary(Nil, Name(""), Nil),
        Template(
          Nil,
          Nil,
          Self(Name(""), None),
          List(
            Defn.Def(
              Nil,
              Term.Name("fx"),
              Nil,
              List(List()),
              Some(Type.Name("Unit")),
              Term.Block(Nil)
            ),
            Defn.Def(
              List(Mod.Private(Name(""))),
              Term.Name("f2"),
              Nil,
              Nil,
              Some(Type.Name("Int")),
              Lit.Int(1)
            )
          )
        )
      )
    )
  }

  test("selftype-class") {
    val code = """|class A extends B:
                  |  thisPhase =>
                  |  expr1
                  |  expr2
                  |""".stripMargin
    val output = """|class A extends B { thisPhase =>
                    |  expr1
                    |  expr2
                    |}
                    |""".stripMargin
    runTestAssert[Stat](code, assertLayout = Some(output))(
      Defn.Class(
        Nil,
        Type.Name("A"),
        Nil,
        Ctor.Primary(Nil, Name(""), Nil),
        Template(
          Nil,
          List(Init(Type.Name("B"), Name(""), Nil)),
          Self(Term.Name("thisPhase"), None),
          List(Term.Name("expr1"), Term.Name("expr2"))
        )
      )
    )
  }

  test("lambda-method") {
    val code = """|object X:
                  |  val fn = (pa, pb) =>
                  |    def helper = 3
                  |    3
                  |  end fn
                  |""".stripMargin
    val output = """|object X {
                    |  val fn = (pa, pb) => {
                    |    def helper = 3
                    |    3
                    |  }
                    |  end fn
                    |}
                    |""".stripMargin
    runTestAssert[Stat](code, assertLayout = Some(output))(
      Defn.Object(
        Nil,
        Term.Name("X"),
        Template(
          Nil,
          Nil,
          Self(Name(""), None),
          List(
            Defn.Val(
              Nil,
              List(Pat.Var(Term.Name("fn"))),
              None,
              Term.Function(
                List(
                  Term.Param(Nil, Term.Name("pa"), None, None),
                  Term.Param(Nil, Term.Name("pb"), None, None)
                ),
                Term.Block(
                  List(Defn.Def(Nil, Term.Name("helper"), Nil, Nil, None, Lit.Int(3)), Lit.Int(3))
                )
              )
            ),
            Term.EndMarker(Term.Name("fn"))
          )
        )
      )
    )
  }

  test("case-for-in-match") {
    val code = """|class A {
                  |    def forward: Unit = parents match
                  |      case a =>
                  |        for case a: TP <- body do
                  |          fordo
                  |      case b => ok
                  |
                  |    private def transformAnnot: Tree
                  |}
                  |""".stripMargin
    val output = """|class A {
                    |  def forward: Unit = parents match {
                    |    case a =>
                    |      for ( case a: TP <- body) fordo
                    |    case b =>
                    |      ok
                    |  }
                    |  private def transformAnnot: Tree
                    |}
                    |""".stripMargin
    runTestAssert[Stat](code, assertLayout = Some(output))(
      Defn.Class(
        Nil,
        Type.Name("A"),
        Nil,
        ctor,
        tpl(
          List(
            Defn.Def(
              Nil,
              Term.Name("forward"),
              Nil,
              Nil,
              Some(Type.Name("Unit")),
              Term.Match(
                Term.Name("parents"),
                List(
                  Case(
                    Pat.Var(Term.Name("a")),
                    None,
                    Term.For(
                      List(
                        Enumerator.CaseGenerator(
                          Pat.Typed(Pat.Var(Term.Name("a")), Type.Name("TP")),
                          Term.Name("body")
                        )
                      ),
                      Term.Name("fordo")
                    )
                  ),
                  Case(Pat.Var(Term.Name("b")), None, Term.Name("ok"))
                )
              )
            ),
            Decl.Def(
              List(Mod.Private(Name(""))),
              Term.Name("transformAnnot"),
              Nil,
              Nil,
              Type.Name("Tree")
            )
          )
        )
      )
    )
  }

  test("this-constructor") {
    val code = """|def this(msg: String) =
                  |  this(message, false)
                  |""".stripMargin
    val output = "def this(msg: String) = this(message, false)"
    runTestAssert[Stat](code, assertLayout = Some(output))(
      Ctor.Secondary(
        Nil,
        Name(""),
        List(List(Term.Param(Nil, Term.Name("msg"), Some(Type.Name("String")), None))),
        Init(
          Type.Singleton(Term.This(Name(""))),
          Name(""),
          List(List(Term.Name("message"), Lit.Boolean(false)))
        ),
        Nil
      )
    )
  }

  test("this-constructor-indented-block") {
    val code = """|def this(msg: String) =
                  |  this(message, false)
                  |  otherStat
                  |""".stripMargin
    val output = """|def this(msg: String) {
                    |  this(message, false)
                    |  otherStat
                    |}
                    |""".stripMargin
    runTestAssert[Stat](code, assertLayout = Some(output))(
      Ctor.Secondary(
        Nil,
        Name(""),
        List(List(Term.Param(Nil, Term.Name("msg"), Some(Type.Name("String")), None))),
        Init(
          Type.Singleton(Term.This(Name(""))),
          Name(""),
          List(List(Term.Name("message"), Lit.Boolean(false)))
        ),
        List(Term.Name("otherStat"))
      )
    )
  }

  test("indent-equals") {
    runTestAssert[Stat](
      """|def genApply() = {
         |      app match {
         |        case Apply2() =>
         |          generatedType =
         |            genTypeApply(t)
         |
         |        case Apply() =>
         |      }
         |} 
         |""".stripMargin,
      assertLayout = Some(
        """|def genApply() = {
           |  app match {
           |    case Apply2() =>
           |      generatedType = genTypeApply(t)
           |    case Apply() =>
           |  }
           |}
           |""".stripMargin
      )
    )(
      Defn.Def(
        Nil,
        Term.Name("genApply"),
        Nil,
        List(List()),
        None,
        Term.Block(
          List(
            Term.Match(
              Term.Name("app"),
              List(
                Case(
                  Pat.Extract(Term.Name("Apply2"), Nil),
                  None,
                  Term.Assign(
                    Term.Name("generatedType"),
                    Term.Apply(Term.Name("genTypeApply"), List(Term.Name("t")))
                  )
                ),
                Case(Pat.Extract(Term.Name("Apply"), Nil), None, Term.Block(Nil))
              ),
              Nil
            )
          )
        )
      )
    )
  }

  test("outdent-with-prev-check") {
    runTestAssert[Stat](
      """|def wrapPlaceholders(t: Tree) = try
         |    if (placeholderParams.isEmpty) t
         |    else new WildcardFunction(placeholderParams.reverse, t)
         |  finally placeholderParams = saved
         |""".stripMargin,
      assertLayout = Some(
        "def wrapPlaceholders(t: Tree) = try if (placeholderParams.isEmpty) t else new WildcardFunction(placeholderParams.reverse, t) finally placeholderParams = saved"
      )
    )(
      Defn.Def(
        Nil,
        Term.Name("wrapPlaceholders"),
        Nil,
        List(List(Term.Param(Nil, Term.Name("t"), Some(Type.Name("Tree")), None))),
        None,
        Term.Try(
          Term.If(
            Term.Select(Term.Name("placeholderParams"), Term.Name("isEmpty")),
            Term.Name("t"),
            Term.New(
              Init(
                Type.Name("WildcardFunction"),
                Name(""),
                List(
                  List(
                    Term.Select(Term.Name("placeholderParams"), Term.Name("reverse")),
                    Term.Name("t")
                  )
                )
              )
            )
          ),
          Nil,
          Some(Term.Assign(Term.Name("placeholderParams"), Term.Name("saved")))
        )
      )
    )
  }

  test("type-in-next-line") {
    runTestAssert[Stat](
      """|abstract class Documentation{
         |
         |  class Graph {
         |    type Node = Int
         |    val a:
         |      Int = 3
         |  }
         |
         |  val refinementTest:
         |    Graph {
         |      def x: Int
         |    }
         |
         |}
         |""".stripMargin,
      assertLayout = Some(
        """|abstract class Documentation {
           |  class Graph {
           |    type Node = Int
           |    val a: Int = 3
           |  }
           |  val refinementTest: Graph { def x: Int }
           |}
           |""".stripMargin
      )
    )(
      Defn.Class(
        List(Mod.Abstract()),
        Type.Name("Documentation"),
        Nil,
        Ctor.Primary(Nil, Name(""), Nil),
        Template(
          Nil,
          Nil,
          Self(Name(""), None),
          List(
            Defn.Class(
              Nil,
              Type.Name("Graph"),
              Nil,
              Ctor.Primary(Nil, Name(""), Nil),
              Template(
                Nil,
                Nil,
                Self(Name(""), None),
                List(
                  Defn.Type(Nil, Type.Name("Node"), Nil, Type.Name("Int")),
                  Defn.Val(Nil, List(Pat.Var(Term.Name("a"))), Some(Type.Name("Int")), Lit.Int(3))
                )
              )
            ),
            Decl.Val(
              Nil,
              List(Pat.Var(Term.Name("refinementTest"))),
              Type.Refine(
                Some(Type.Name("Graph")),
                List(Decl.Def(Nil, Term.Name("x"), Nil, Nil, Type.Name("Int")))
              )
            )
          )
        )
      )
    )
  }

  test("type-in-next-line-equals") {
    runTestAssert[Stat](
      """|val refinementTest:
         |      Int = 3
         |""".stripMargin,
      assertLayout = Some("val refinementTest: Int = 3")
    )(
      Defn.Val(Nil, List(Pat.Var(Term.Name("refinementTest"))), Some(Type.Name("Int")), Lit.Int(3))
    )
  }

  test("type-in-next-line-equals-newline") {
    runTestAssert[Stat](
      """|val refinementTest:
         |      Int = 
         |3
         |""".stripMargin,
      assertLayout = Some("val refinementTest: Int = 3")
    )(
      Defn.Val(Nil, List(Pat.Var(Term.Name("refinementTest"))), Some(Type.Name("Int")), Lit.Int(3))
    )
  }

  test("type-equals-separate") {
    runTestAssert[Stat](
      """|def refinementTest(a :
         |      Int = 
         |3) = a
         |""".stripMargin,
      assertLayout = Some("def refinementTest(a: Int = 3) = a")
    )(
      Defn.Def(
        Nil,
        Term.Name("refinementTest"),
        Nil,
        List(List(Term.Param(Nil, Term.Name("a"), Some(Type.Name("Int")), Some(Lit.Int(3))))),
        None,
        Term.Name("a")
      )
    )
  }

  test("type-multi-seq") {
    runTestAssert[Stat](
      """|val refinementTest:
         |    Int = 
         |  fx
         |  fy
         |""".stripMargin,
      assertLayout = Some("val refinementTest: Int = {\n  fx\n  fy\n}")
    )(
      Defn.Val(
        Nil,
        List(Pat.Var(Term.Name("refinementTest"))),
        Some(Type.Name("Int")),
        Term.Block(List(Term.Name("fx"), Term.Name("fy")))
      )
    )
  }

  test("equals-block") {
    runTestAssert[Stat](
      """|val refinementTest:
         |  Int = 
         |    fx
         |    fy
         |""".stripMargin,
      assertLayout = Some("val refinementTest: Int = {\n  fx\n  fy\n}")
    )(
      Defn.Val(
        Nil,
        List(Pat.Var(Term.Name("refinementTest"))),
        Some(Type.Name("Int")),
        Term.Block(List(Term.Name("fx"), Term.Name("fy")))
      )
    )
  }

  test("given-block-indent") {
    runTestAssert[Stat](
      """|given intOrd: Ord[Int] with
         |   def fa: Int = 1
         |   def fb: Int = 2
         |""".stripMargin,
      assertLayout = Some(
        """|given intOrd: Ord[Int] with {
           |  def fa: Int = 1
           |  def fb: Int = 2
           |}""".stripMargin
      )
    )(
      Defn.Given(
        Nil,
        Term.Name("intOrd"),
        Nil,
        Nil,
        Template(
          Nil,
          List(Init(Type.Apply(Type.Name("Ord"), List(Type.Name("Int"))), Name(""), Nil)),
          Self(Name(""), None),
          List(
            Defn.Def(Nil, Term.Name("fa"), Nil, Nil, Some(Type.Name("Int")), Lit.Int(1)),
            Defn.Def(Nil, Term.Name("fb"), Nil, Nil, Some(Type.Name("Int")), Lit.Int(2))
          ),
          Nil
        )
      )
    )
  }

  test("given-block-indent-edge-cases") {
    runTestError[Stat](
      """|given intOrd: Ord[Int] with
         |def fa: Int = 1
         |def fb: Int = 2
         |""".stripMargin,
      "expected '{' or indentation"
    )

    runTestError[Stat](
      """|given intOrd: Ord[Int]:
         |  def fa: Int = 1
         |  def fb: Int = 2
         |""".stripMargin,
      "; expected but : found"
    )

    runTestAssert[Stat](
      """|class A extends A with 
         |  B
         |""".stripMargin,
      assertLayout = Some("class A extends A with B")
    )(
      Defn.Class(
        Nil,
        Type.Name("A"),
        Nil,
        Ctor.Primary(Nil, Name(""), Nil),
        Template(
          Nil,
          List(Init(Type.Name("A"), Name(""), Nil), Init(Type.Name("B"), Name(""), Nil)),
          Self(Name(""), None),
          Nil,
          Nil
        )
      )
    )
  }

  test("nested-coloneol") {
    runTestAssert[Stat](
      """|case class Test(
         |  a: A = new A,
         |):
         |  def hello = 1
         |""".stripMargin,
      assertLayout = Some("case class Test(a: A = new A) { def hello = 1 }")
    )(
      Defn.Class(
        List(Mod.Case()),
        Type.Name("Test"),
        Nil,
        Ctor.Primary(
          Nil,
          Name(""),
          List(
            List(
              Term.Param(
                Nil,
                Term.Name("a"),
                Some(Type.Name("A")),
                Some(Term.New(Init(Type.Name("A"), Name(""), Nil)))
              )
            )
          )
        ),
        Template(
          Nil,
          Nil,
          Self(Name(""), None),
          List(Defn.Def(Nil, Term.Name("hello"), Nil, Nil, None, Lit.Int(1)))
        )
      )
    )
  }

  test("semicolon-closes-indent-region") {
    runTestAssert[Stat](
      """|val z =
         |  val a = 
         |    0;
         |  f(a)
         |""".stripMargin,
      assertLayout = Some(
        """|val z = {
           |  val a = 0
           |  f(a)
           |}""".stripMargin
      )
    )(
      Defn.Val(
        Nil,
        List(Pat.Var(Term.Name("z"))),
        None,
        Term.Block(
          List(
            Defn.Val(Nil, List(Pat.Var(Term.Name("a"))), None, Lit.Int(0)),
            Term.Apply(Term.Name("f"), List(Term.Name("a")))
          )
        )
      )
    )
  }

  test("observe-indented-in-braces") {
    val code = """|object X:
                  |  if (cond)
                  |    (f)
                  |  foo
                  |""".stripMargin
    val output = """|object X {
                    |  if (cond) f
                    |  foo
                    |}
                    |""".stripMargin
    runTestAssert[Stat](code, assertLayout = Some(output))(
      Defn.Object(
        Nil,
        Term.Name("X"),
        Template(
          Nil,
          Nil,
          Self(Name(""), None),
          List(
            Term.If(Term.Name("cond"), Term.Name("f"), Lit.Unit(), Nil),
            Term.Name("foo")
          ),
          Nil
        )
      )
    )
  }

  test("match-case-same-line") {
    runTestAssert[Stat](
      """|widen match
         |  case tp @ OrNull(tp1): OrType =>
         |  case tp => tp
         |""".stripMargin,
      assertLayout = Some(
        """|widen match {
           |  case (tp @ OrNull(tp1)): OrType =>
           |  case tp => tp
           |}
           |""".stripMargin
      )
    )(
      Term.Match(
        Term.Name("widen"),
        List(
          Case(
            Pat.Typed(
              Pat.Bind(
                Pat.Var(Term.Name("tp")),
                Pat.Extract(Term.Name("OrNull"), List(Pat.Var(Term.Name("tp1"))))
              ),
              Type.Name("OrType")
            ),
            None,
            Term.Block(Nil)
          ),
          Case(Pat.Var(Term.Name("tp")), None, Term.Name("tp"))
        ),
        Nil
      )
    )
  }

  test("object-type") {
    runTestAssert[Stat](
      """|object typeAndObjects:
         |  type Ala
         |""".stripMargin,
      assertLayout = Some(
        "object typeAndObjects { type Ala }"
      )
    )(
      Defn.Object(
        Nil,
        Term.Name("typeAndObjects"),
        Template(
          Nil,
          Nil,
          Self(Name(""), None),
          List(Decl.Type(Nil, Type.Name("Ala"), Nil, Type.Bounds(None, None))),
          Nil
        )
      )
    )
  }

  test("old-try-catch-same-indent") {
    val code = """|try 
                  |  fx
                  |  gx 
                  |  catch 
                  |  case aa =>
                  |  case bb =>
                  |  finally
                  |  cc
                  |  dd
                  |""".stripMargin
    val output = """|try {
                    |  fx
                    |  gx
                    |} catch {
                    |  case aa =>
                    |  case bb =>
                    |} finally {
                    |  cc
                    |  dd
                    |}
                    |""".stripMargin
    runTestAssert[Stat](code, assertLayout = Some(output))(
      Term.Try(
        Term.Block(List(Term.Name("fx"), Term.Name("gx"))),
        List(
          Case(Pat.Var(Term.Name("aa")), None, Term.Block(Nil)),
          Case(Pat.Var(Term.Name("bb")), None, Term.Block(Nil))
        ),
        Some(Term.Block(List(Term.Name("cc"), Term.Name("dd"))))
      )
    )
  }

  test("if-else-same-indent") {
    val code = """|if 
                  |  cond
                  |  cond2
                  |  then 
                  |  fx
                  |  gx
                  |  else gx
                  |""".stripMargin
    val output = """|if ({
                    |  cond
                    |  cond2
                    |}) {
                    |  fx
                    |  gx
                    |} else gx
                    |""".stripMargin
    runTestAssert[Stat](code, assertLayout = Some(output))(
      Term.If(
        Term.Block(List(Term.Name("cond"), Term.Name("cond2"))),
        Term.Block(List(Term.Name("fx"), Term.Name("gx"))),
        Term.Name("gx"),
        Nil
      )
    )
  }

  test("new-fordo-same-indent") {
    val code = """|for
                  |  a <- gen
                  |  do fx
                  |""".stripMargin
    val output = "for (a <- gen) fx"
    runTestAssert[Stat](code, assertLayout = Some(output))(
      Term.For(
        List(Enumerator.Generator(Pat.Var(Term.Name("a")), Term.Name("gen"))),
        Term.Name("fx")
      )
    )
  }

  test("new-for-yield-same-indent") {
    val code = """|for
                  |  a <- x
                  |  b <- y
                  |  yield fx
                  |""".stripMargin
    val output = "for (a <- x; b <- y) yield fx"
    runTestAssert[Stat](code, assertLayout = Some(output))(
      Term.ForYield(
        List(
          Enumerator.Generator(Pat.Var(Term.Name("a")), Term.Name("x")),
          Enumerator.Generator(Pat.Var(Term.Name("b")), Term.Name("y"))
        ),
        Term.Name("fx")
      )
    )
  }

  test("match-chained-same-indent") {
    runTestAssert[Stat](
      """|val hello = xs match
         |  case Nil => "empty"
         |  case x :: xs1 => "nonempty"
         |  match
         |  case true => 0
         |  case false => 1
         |
         |""".stripMargin,
      assertLayout = None
    )(
      Defn.Val(
        Nil,
        List(Pat.Var(Term.Name("hello"))),
        None,
        Term.Match(
          Term.Match(
            Term.Name("xs"),
            List(
              Case(Term.Name("Nil"), None, Lit.String("empty")),
              Case(
                Pat.ExtractInfix(
                  Pat.Var(Term.Name("x")),
                  Term.Name("::"),
                  List(Pat.Var(Term.Name("xs1")))
                ),
                None,
                Lit.String("nonempty")
              )
            ),
            Nil
          ),
          List(
            Case(Lit.Boolean(true), None, Lit.Int(0)),
            Case(Lit.Boolean(false), None, Lit.Int(1))
          ),
          Nil
        )
      )
    )

  }

  test("match-if-indent") {
    runTestAssert[Stat](
      """|def hackGetmembers = a match
         |   case sym if
         |     cond =>
         |     sym
         |
         |""".stripMargin,
      assertLayout = Some(
        """|def hackGetmembers = a match {
           |  case sym if cond => sym
           |}
           |""".stripMargin
      )
    )(
      Defn.Def(
        Nil,
        Term.Name("hackGetmembers"),
        Nil,
        Nil,
        None,
        Term.Match(
          Term.Name("a"),
          List(Case(Pat.Var(Term.Name("sym")), Some(Term.Name("cond")), Term.Name("sym"))),
          Nil
        )
      )
    )

  }
  test("match-empty") {
    runTestAssert[Stat](
      """|def mapSymbols =
         |  originals.foreach { a =>
         |    copy.denot match
         |      case cd: ClassDenotation =>
         |      case _ =>
         |  }
         |""".stripMargin,
      assertLayout = Some(
        """|def mapSymbols = originals.foreach {
           |  a => copy.denot match {
           |    case cd: ClassDenotation =>
           |    case _ =>
           |  }
           |}
           |""".stripMargin
      )
    )(
      Defn.Def(
        Nil,
        Term.Name("mapSymbols"),
        Nil,
        Nil,
        None,
        Term.Apply(
          Term.Select(Term.Name("originals"), Term.Name("foreach")),
          List(
            Term.Block(
              List(
                Term.Function(
                  List(Term.Param(Nil, Term.Name("a"), None, None)),
                  Term.Match(
                    Term.Select(Term.Name("copy"), Term.Name("denot")),
                    List(
                      Case(
                        Pat.Typed(Pat.Var(Term.Name("cd")), Type.Name("ClassDenotation")),
                        None,
                        Term.Block(Nil)
                      ),
                      Case(Pat.Wildcard(), None, Term.Block(Nil))
                    ),
                    Nil
                  )
                )
              )
            )
          )
        )
      )
    )

  }

  test("return-indent") {
    runTestAssert[Stat](
      """|def method =
         |   return
         |     val a = 2 + 3
         |     a
         |
         |""".stripMargin,
      assertLayout = Some(
        """|def method = return {
           |  val a = 2 + 3
           |  a
           |}
           |""".stripMargin
      )
    )(
      Defn.Def(
        Nil,
        Term.Name("method"),
        Nil,
        Nil,
        None,
        Term.Return(
          Term.Block(
            List(
              Defn.Val(
                Nil,
                List(Pat.Var(Term.Name("a"))),
                None,
                Term.ApplyInfix(Lit.Int(2), Term.Name("+"), Nil, List(Lit.Int(3)))
              ),
              Term.Name("a")
            )
          )
        )
      )
    )
  }

  test("return-single-indent") {
    runTestAssert[Stat](
      """|def method =
         |   return
         |     2 
         |     + 3
         |""".stripMargin,
      assertLayout = Some(
        "def method = return 2 + 3"
      )
    )(
      Defn.Def(
        Nil,
        Term.Name("method"),
        Nil,
        Nil,
        None,
        Term.Return(
          Term.ApplyInfix(Lit.Int(2), Term.Name("+"), Nil, List(Lit.Int(3)))
        )
      )
    )
  }

  test("empty-return") {
    runTestAssert[Stat](
      """|    def skip = {
         |        token match {
         |          case RBRACE =>
         |            if (true)
         |              return
         |            change(-1)
         |          case LBRACE =>
         |             if (true)
         |               return
         |             change(-1)
         |        }
         |    }
         |""".stripMargin,
      assertLayout = Some(
        """|def skip = {
           |  token match {
           |    case RBRACE =>
           |      if (true) return
           |      change(-1)
           |    case LBRACE =>
           |      if (true) return
           |      change(-1)
           |  }
           |}
           |""".stripMargin
      )
    )(
      Defn.Def(
        Nil,
        Term.Name("skip"),
        Nil,
        Nil,
        None,
        Term.Block(
          List(
            Term.Match(
              Term.Name("token"),
              List(
                Case(
                  Term.Name("RBRACE"),
                  None,
                  Term.Block(
                    List(
                      Term.If(Lit.Boolean(true), Term.Return(Lit.Unit()), Lit.Unit(), Nil),
                      Term.Apply(Term.Name("change"), List(Lit.Int(-1)))
                    )
                  )
                ),
                Case(
                  Term.Name("LBRACE"),
                  None,
                  Term.Block(
                    List(
                      Term.If(Lit.Boolean(true), Term.Return(Lit.Unit()), Lit.Unit(), Nil),
                      Term.Apply(Term.Name("change"), List(Lit.Int(-1)))
                    )
                  )
                )
              ),
              Nil
            )
          )
        )
      )
    )
  }

  test("case-block") {
    runTestAssert[Stat](
      """|val success = suffixes.find { suffix =>
         |  try {
         |    true
         |  } catch {
         |    case e: StorageException =>
         |      false
         |  }
         |}
         |""".stripMargin,
      assertLayout = Some(
        """|val success = suffixes.find {
           |  suffix => try {
           |    true
           |  } catch {
           |    case e: StorageException => false
           |  }
           |}
           |""".stripMargin
      )
    )(
      Defn.Val(
        Nil,
        List(Pat.Var(Term.Name("success"))),
        None,
        Term.Apply(
          Term.Select(Term.Name("suffixes"), Term.Name("find")),
          List(
            Term.Block(
              List(
                Term.Function(
                  List(Term.Param(Nil, Term.Name("suffix"), None, None)),
                  Term.Try(
                    Term.Block(List(Lit.Boolean(true))),
                    List(
                      Case(
                        Pat.Typed(Pat.Var(Term.Name("e")), Type.Name("StorageException")),
                        None,
                        Lit.Boolean(false)
                      )
                    ),
                    None
                  )
                )
              )
            )
          )
        )
      )
    )
  }

  test("complext-match-else") {
    runTestAssert[Stat](
      """|val calleeType = a match {
         |  case _ =>
         |    if cond then
         |      expr match
         |        case _ =>
         |          f
         |    else NoType
         |  case _ =>
         |     NoType
         |}
         |""".stripMargin,
      assertLayout = Some(
        """|val calleeType = a match {
           |  case _ =>
           |    if (cond) expr match {
           |      case _ => f
           |    } else NoType
           |  case _ =>
           |    NoType
           |}
           |""".stripMargin
      )
    )(
      Defn.Val(
        Nil,
        List(Pat.Var(Term.Name("calleeType"))),
        None,
        Term.Match(
          Term.Name("a"),
          List(
            Case(
              Pat.Wildcard(),
              None,
              Term.If(
                Term.Name("cond"),
                Term
                  .Match(Term.Name("expr"), List(Case(Pat.Wildcard(), None, Term.Name("f"))), Nil),
                Term.Name("NoType"),
                Nil
              )
            ),
            Case(Pat.Wildcard(), None, Term.Name("NoType"))
          ),
          Nil
        )
      )
    )
  }
}
