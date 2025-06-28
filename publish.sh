#!/bin/bash

# publish.sh
# 하드웨어 스펙 관리 프레임워크의 모든 구성 요소를 로컬 Maven/Ivy 저장소에 발행하는 스크립트입니다.
# 이 스크립트는 빌드 환경을 깨끗하게 초기화하고, 올바른 의존성 순서에 따라 프로젝트를 빌드합니다.

# 1. 모든 SBT 관련 캐시 및 빌드 아티팩트 삭제 (강력한 클린업)
echo "--- 1/5: 모든 SBT 캐시 및 빌드 아티팩트 삭제 중 ---"
rm -rf target/ project/target/
rm -rf spec-core/target/ spec-macros/target/ spec-plugin/target/ design/target/
rm -rf .bloop/
rm -rf ~/.ivy2/local/your.company/ # 로컬 발행된 'your.company' 아티팩트 강제 삭제
rm -rf ~/.sbt/boot/ ~/.sbt/cache/ ~/.sbt/plugin/ # SBT 부트 및 전역 캐시 삭제
echo "캐시 및 아티팩트 삭제 완료."
echo ""

# 2. spec-core 로컬 발행 (모든 크로스 Scala 버전 포함)
# spec-macros와 spec-plugin이 spec-core에 의존하므로 가장 먼저 발행해야 합니다.
echo "--- 2/5: spec-core 로컬 발행 중 (모든 Scala 버전) ---"
sbt "+specCore / publishLocal"
if [ $? -ne 0 ]; then
    echo "오류: spec-core 발행 실패. 스크립트를 중단합니다."
    exit 1
fi
echo "spec-core 발행 완료."
echo ""

# 3. spec-macros 로컬 발행 (spec-core에 의존)
# design 프로젝트가 spec-macros에 의존하므로 spec-core 다음으로 발행해야 합니다.
echo "--- 3/5: spec-macros 로컬 발행 중 (주요 Scala 버전) ---"
sbt "+specMacros / publishLocal" # spec-macros는 ThisBuild/scalaVersion에 따라 2.13으로만 발행됨 (crossScalaVersions := Seq("2.13.12"))
if [ $? -ne 0 ]; then
    echo "오류: spec-macros 발행 실패. 스크립트를 중단합니다."
    exit 1
fi
echo "spec-macros 발행 완료."
echo ""


# 4. spec-plugin 로컬 발행 (spec-core에 의존)
# design 프로젝트가 spec-plugin을 플러그인으로 로드하므로 마지막으로 발행해야 합니다.
echo "--- 4/5: spec-plugin 로컬 발행 중 ---"
# sbt 쉘에서 프로젝트 전환 후 publishLocal 실행
sbt <<EOF
project specPlugin
publishLocal
exit
EOF
if [ $? -ne 0 ]; then
    echo "오류: spec-plugin 발행 실패. 스크립트를 중단합니다."
    exit 1
fi
echo "spec-plugin 발행 완료."
echo ""

# 5. design 프로젝트 컴파일 및 스펙 인덱스 생성 (독립적으로)
# design 프로젝트는 이제 완전히 독립적인 SBT 프로젝트입니다.
echo "--- 5/5: design 프로젝트 컴파일 및 스펙 인덱스 생성 중 ---"
cd design
sbt compile # design 프로젝트 컴파일
if [ $? -ne 0 ]; then
    echo "오류: design 프로젝트 컴파일 실패. 스크립트를 중단합니다."
    exit 1
fi

sbt "exportSpecIndex" # 스펙 인덱스 생성
if [ $? -ne 0 ]; then
    echo "오류: exportSpecIndex 실행 실패. 스크립트를 중단합니다."
    exit 1
fi
cd .. # 루트 디렉토리로 돌아가기

echo ""
echo "--- 모든 빌드 및 발행 과정 완료 ---"
echo "design/target/ 디렉토리에서 SpecIndex.json 및 ModuleIndex.json 파일을 확인하세요."
