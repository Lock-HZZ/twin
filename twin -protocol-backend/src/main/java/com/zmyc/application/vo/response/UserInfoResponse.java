package com.zmyc.application.vo.response;

import lombok.Data;

@Data
public class UserInfoResponse {

    private Long id;

    private String address;

    private String email;

    private Byte enabled;

    private String invitedCode;
}
