package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.*;
import com.cst438.service.RegistrarServiceProxy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EnrollmentControllerUnitTest {

   @Autowired
    private WebTestClient client ;
    @Autowired
    private SectionRepository sectionRepository;

    // default behavior for a Mock bean
    // return 0 or null for a method that returns a value
    //for method that returns void, the mock method records the call but does nothing
    @MockitoBean
    RegistrarServiceProxy registrarService;
    Random random = new Random();

    @Test
    public void sectionCreateUpdateDelete() throws Exception {

        // login as admin and get the security token
        String instructorEmail = "ted@csumb.edu";
        String password = "ted2025";

        EntityExchangeResult<LoginDTO> login_dto =  client.get().uri("/login")
                .headers(headers -> headers.setBasicAuth(instructorEmail, password))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class).returnResult();

        String jwt = login_dto.getResponseBody().jwt();
        assertNotNull(jwt);


        EntityExchangeResult<List<EnrollmentDTO>> enrollmentResponse =  client.get().uri("/sections/1/enrollments")
                .headers(headers -> headers.setBearerAuth(jwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(EnrollmentDTO.class).returnResult();






    }

}