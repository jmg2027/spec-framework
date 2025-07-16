# Scaladoc and File Header Guide

This guide describes how to document Scala source files in this repository.

## 1. File Header
Each Scala file should begin with a short comment block describing its purpose.

```scala
// [relative/path/FileName.scala]
// Brief description in one line
// (Optional) additional context or notes
```

## 2. Scaladoc Format
Use standard Scaladoc comment blocks for all public classes, objects and methods.

```scala
/**
 * Summary of the entity
 *
 * Additional details about behavior and usage.
 *
 * Usage:
 *   example code
 *
 * Key points:
 *   - bullet 1
 *   - bullet 2
 *
 * @param ... description of parameters
 * @return    description of return value
 */
```

## 3. Recommendations
- Write comments in English.
- Document the intent of each module, especially macros and sbt tasks.
- Keep lines under 100 characters; configure scalafmt accordingly.

## 4. Automation Tools
- [scalafmt](https://scalameta.org/scalafmt/) for code formatting
- [scalastyle](http://www.scalastyle.org/) to check for missing doc comments

## 5. scalafmt Template
Place the following as `.scalafmt.conf` in the repository root.

```hocon
version = 3.7.14
maxColumn = 100
align = most
newlines.topLevelStatements = [before]
newlines.implicitParamListModifier = true
docstrings = ScalaDoc
rewrite.rules = [RedundantBraces, SortImports]
trailingCommas = always
indentOperator = spray
continuationIndent.callSite = 2
continuationIndent.defnSite = 2
```
