# 🧠 NeckFree : AI 기반 자세 교정 앱
> MediaPipe 기반 상체 자세 인식 & 실시간 피드백 시스템  
> Android Studio (Kotlin) + Google MediaPipe + CameraX

---

## 📱 프로젝트 개요
**NeckFree**는 스마트폰 카메라를 이용해 사용자의 **상체 자세를 인식하고,  
거북목·굽은 어깨 등의 불균형 자세를 실시간으로 교정**하도록 돕는 AI 헬스케어 앱입니다.

사용자는 카메라 앞에 서면 앱이 **어깨·귀·허리의 관절 포인트를 감지**하고,  
AI가 계산한 각도에 따라 다음과 같은 피드백을 제공합니다.

- 🟢 **올바른 자세입니다!**  
- 🟡 **살짝 고개가 앞으로 나왔어요.**  
- 🔴 **거북목 자세입니다! 주의하세요.**

---

## 🧩 주요 기능
| 기능 | 설명 |
|------|------|
| 🎥 **실시간 카메라 입력** | CameraX를 이용해 실시간 상체 인식 |
| 🤖 **MediaPipe Pose Detection** | BlazePose 모델 기반 관절 추출 |
| 🧠 **AI 자세 분석 알고리즘** | 귀-어깨-허리 간 각도 계산으로 자세 판별 |
| 🗣 **피드백 출력** | 화면/텍스트/음성으로 교정 안내 |
| 🧾 **결과 로그** | 일별 평균 자세 점수 저장 (향후 RoomDB 예정) |

---

## ⚙️ 기술 스택
| 구분 | 기술 |
|------|------|
| Language | Kotlin |
| Framework | Android Studio (Jetpack) |
| AI Library | Google MediaPipe Tasks (BlazePose) |
| Camera | AndroidX CameraX |
| Build | Gradle (Groovy DSL) |
| Version Control | Git & GitHub |

---

## 🧠 AI 자세 분석 알고리즘 요약

```text
입력: 상체 관절 좌표 (귀, 어깨, 허리)
1️⃣ 귀-어깨, 어깨-허리 벡터 계산
2️⃣ 두 벡터의 내적을 통해 목 각도 산출
3️⃣ 기준 각도(10°, 20°)에 따라 상태 구분:
    <10° → 정상
    10°~20° → 주의
    >20° → 거북목
4️⃣ 실시간 피드백으로 표시
