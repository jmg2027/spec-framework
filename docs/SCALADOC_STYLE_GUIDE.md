# Scaladoc & File Header Style Guide

## 1. File Header (모든 Scala 파일 최상단)
```scala
// [상대경로/파일명.scala]
// 간단한 한 줄 설명 (영문)
// (필요시) 여러 줄로 주요 목적/역할 요약
```
**예시:**
```scala
// spec-macros/src/main/scala-2.13/framework/macros/LocalSpec.scala
// Macro annotation for tagging hardware modules/specs with a spec ID (@LocalSpec)
// Emits Tag metadata at compile time for plugin aggregation and registry.
```

---

## 2. Scaladoc (클래스/오브젝트/메서드)
```scala
/**
 * [클래스/메서드명]: 한 줄 요약
 *
 * 주요 목적/동작/사용법을 상세히 기술
 *
 * Usage:
 *   예시 코드
 *
 * 주요 동작:
 *   - 동작1
 *   - 동작2
 *
 * @param ...  파라미터 설명
 * @return     반환값 설명
 * @throws     예외 상황 (필요시)
 * @see        참고 링크/파일 (필요시)
 */
```
**예시:**
```scala
/**
 * LocalSpec: Macro annotation for tagging classes, objects, vals, or defs with a hardware spec ID.
 *
 * Usage:
 *   @LocalSpec("MY_SPEC_ID")
 *   class MyModule { ... }
 *
 * This macro will:
 *   - Enforce that the argument is a string literal (the spec ID, must be unique per compile run)
 *   - Prevent duplicate IDs within the same compilation run (compile-time error)
 *   - At compile time, emit a Tag metadata file (for plugin aggregation)
 *   - Register the tag in SpecRegistry for runtime/debug use
 *   - Support annotation of class, object, val, or def (injects side effect in companion or body)
 *
 * See design/build.sbt for required macro-annotation settings.
 */
```

---

## 3. 기타 권장 사항
- 모든 public class/object/def에 Scaladoc을 작성
- 파일 헤더와 Scaladoc은 영어로 작성 (팀 내 합의시 한글 병행 가능)
- 예시/용례/동작/파라미터/반환/예외/참고 등 최대한 구체적으로
- 반복되는 패턴은 IDE 스니펫/템플릿으로 등록 권장

---

## 4. 참고: 자동화 도구
- [scalafmt](https://scalameta.org/scalafmt/)로 코드 스타일 자동화
- [scalastyle](http://www.scalastyle.org/)로 Scaladoc 누락 등 체크 가능

---

## 5. scalafmt 기본 템플릿 예시
아래 파일을 `.scalafmt.conf`로 루트에 두세요.

```hocon
version = 3.7.14
maxColumn = 100
align = most
newlines.topLevelStatements = [before]
newlines.implicitParamListModifier = true
docstrings = ScalaDoc
rewrite.rules = [RedundantBraces, SortImports]
spaces.inImportCurlyBraces = false
trailingCommas = always
includeCurlyBraceInSelectChains = false
indentOperator = spray
continuationIndent.callSite = 2
continuationIndent.defnSite = 2
```

---

## 6. 예시 파일/템플릿
- 이 파일(`SCALADOC_STYLE_GUIDE.md`)을 참고해 새 파일을 작성하세요.
- 잘 작성된 예시 파일을 `docs/example/`에 추가해두면 좋습니다.
