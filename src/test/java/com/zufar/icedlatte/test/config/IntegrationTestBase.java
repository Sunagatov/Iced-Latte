package com.zufar.icedlatte.test.config;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.zufar.icedlatte.filestorage.service.ObjectStorage;

public abstract class IntegrationTestBase extends ContainerizedIntegrationTestBase {

    @MockitoBean
    protected JavaMailSender javaMailSender;

    @MockitoBean
    protected ObjectStorage objectStorage;
}
