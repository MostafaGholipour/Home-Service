package com.example.homeService.dto;

import lombok.*;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InputJobDto {
    String customerUsername;
    Long orderId;

}
