---
name: Product testing report
about: Product testing report is used in the release phase. 
title: "Ohara ${version} product testing report"
labels: umbrella
assignees: ''

---

## 測試項目

<details>
<summary><b>1. 基本測試</b></summary>

- [ ] 確認畫面（UI）與設計稿（Design）是否一致
  - 顏色、字型、符號、圖片、...
- [ ] 檢查所有畫面的 Title 名稱是否正確
- [ ] 所有連結是否有前往正確頁面
- [ ] 切換視窗大小時，有無跑版情形
- [ ] 切換視窗大小後，文字與圖形是否竄行
- 表單
  - [ ] 任何一個必填欄位未填寫，不能送出
  - [ ] 表單送出中，應該明確表達正在處理中的訊號
  - [ ] 表單送出中，應該要凍結表單（除非有提供取消功能）
  - [ ] 任何表單至少要有 CTA 及 Cancel 兩類按紐 (CTA: Call-To-Action)
- 訊息
  - [ ] 成功訊息，應該要明確指出完成對象
  - [ ] 錯誤訊息，應該要明確指出錯誤對象
  - [ ] 確認訊息
    - [ ] 刪除確認，應該要明確指出欲刪除對象
- 按鈕
  - [ ] 任何一個刪除按鈕，都要有刪除確認提示
- 鼠標
  - [ ] 移動到所有可以點擊的元素，要出現 pointer cursor
      - Button, Link icon, Link text, ...
</details>

<details>
<summary><b>2. 功能測試: <a href="https://github.com/oharastream/ohara/wiki/Test-case:-Normal-case">Normal case</a></b></summary>

- [ ] Create a new workspace with three nodes
- [ ] Create two topic and upload a stream app jar into the workspace
- [ ] Create a new pipeline and add some components into pipeline
- [ ] 連接 FTP Source -> Topic 1 -> Stream App -> Topic 2 -> HDFS Sink
- [ ] Prepare required folders and test data on the FTP server
- [ ] Start all connectors and stream app
- [ ] Verify which test data was successfully dumped to HDFS
</details>

## 測試所發現的問題紀錄

### 設計稿：
- (設計稿 Issue Number 紀錄在此)

### 測試環境：
- Ohara
  - Mode: (ssh or k8s)
  - Version: xxx
  - Build date: xxx
- 測試工具
  - 瀏覽器：xxx
  - 最小螢幕寬度：xxx

### 基本測試：
- (基本測試所發現的問題，紀錄在此)

### 功能測試：
- (功能測試所發現的問題，紀錄在此)
