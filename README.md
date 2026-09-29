# MineGuard IDR

AI-Assisted Inertial Dead Reckoning Android Navigation System

## Problem
Maintains vehicle navigation during GNSS outages.

## Architecture
Phone MEMS IMU
↓
15-feature preprocessing
↓
30-sample sequence
↓
GRU model
(Predicts vehicle velocity & yaw rate)
↓
NHC & ZUPT Engine 
(Clamps static drift; removes gyro bias & noise)
↓
Dead Reckoning
↓
GNSS outage/recovery manager
↓
Offline Map

## AI Model
- 2-layer GRU
- 64 hidden units
- 44,932 learned parameters
- Input: [1, 30, 15]
- Output: [1, 4]
- ONNX Runtime Android

## Tech Stack
- Kotlin
- Python
- ONNX Runtime
- Room
- DataStore
- MapLibre

## Outputs
- Vehicle velocity
- Longitudinal acceleration
- Lateral acceleration
- Yaw rate
- Dead-reckoned position