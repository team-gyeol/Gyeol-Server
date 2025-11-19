package com.example.oauth2prac.domain.segmentedImage;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SegmentationResponseDTO {
    @JsonProperty("analysisResult")
    private String analysisResult;

    @JsonProperty("segmentedImageUrl")
    private String segmentedImageUrl;

    private Integer multicopterBodyCount = 0;
    private Integer propellerCount = 0;
    private Integer cameraCount = 0;
    private Integer legCount = 0;


    public void parseAnalysisResult() {
        if (analysisResult == null || analysisResult.isEmpty() || 
            "탐지된 객체가 없습니다.".equals(analysisResult)) {
            return;
        }

        // 파싱
        String[] parts = analysisResult.split(", ");
        for (String part : parts) {
            if (part.contains(":")) {
                String[] keyValue = part.split(":");
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim();
                    String value = keyValue[1].trim().replace("개", "").trim();
                    
                    try {
                        int count = Integer.parseInt(value);
                        
                        switch (key) {
                            case "multicopter_body":
                                this.multicopterBodyCount = count;
                                break;
                            case "propeller":
                                this.propellerCount = count;
                                break;
                            case "camera":
                                this.cameraCount = count;
                                break;
                            case "leg":
                                this.legCount = count;
                                break;
                        }
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("숫자 파싱 실패: " + e.getMessage());
                    }
                }
            }
        }
    }
}
