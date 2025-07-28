package com.cst438.controller;

import com.cst438.service.RegistrarServiceProxy;
import com.cst438.dto.AssignmentDTO;
import com.cst438.dto.AssignmentStudentDTO;
import com.cst438.dto.LoginDTO;
import com.cst438.domain.Assignment;
import com.cst438.domain.AssignmentRepository;
import com.cst438.domain.Section;
import com.cst438.domain.SectionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AssignmentControllerUnitTest {

    @Autowired
    private WebTestClient client;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @MockitoBean
    RegistrarServiceProxy registrarServiceProxy;

    private Random random = new Random();

    /**
     * Helper to perform login and extract JWT token.
     */
    private String loginAndGetJwt(String email, String password) {
        EntityExchangeResult<LoginDTO> loginResult = client.get().uri("/login")
                .headers(h -> h.setBasicAuth(email, password))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class)
                .returnResult();
        String jwt = loginResult.getResponseBody().jwt();
        assertNotNull(jwt, "JWT token should not be null for " + email);
        return jwt;
    }

    @Test
    public void assignmentCreateUpdateDelete() throws Exception {
        // login as instructor
        String jwt = loginAndGetJwt("ted@csumb.edu", "ted2025");

        // fetch an existing section
        List<Section> sections = new ArrayList<>();
        sectionRepository.findAll().forEach(sections::add);
        assertFalse(sections.isEmpty(), "At least one Section must exist in test DB");
        Section firstSection = sections.get(0);
        int sectionId = firstSection.getSectionId();
        int sectionNo = firstSection.getSectionNo();
        String courseId = firstSection.getCourse().getCourseId();

        // create assignment
        AssignmentDTO createDto = new AssignmentDTO(
                0,
                "Test Assignment",
                "2025-10-01",
                courseId,
                sectionId,
                sectionNo
        );

        EntityExchangeResult<AssignmentDTO> createResult = client.post().uri("/assignments")
                .headers(h -> h.setBearerAuth(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AssignmentDTO.class)
                .returnResult();

        AssignmentDTO saved = createResult.getResponseBody();
        assertNotNull(saved);
        assertTrue(saved.id() > 0, "Assignment ID should be assigned");
        assertTrue(assignmentRepository.findById(saved.id()).isPresent());

        // update assignment
        AssignmentDTO updateDto = new AssignmentDTO(
                saved.id(),
                "Updated Test Assignment",
                "2025-10-02",
                saved.courseId(),
                saved.secId(),
                saved.secNo()
        );

        client.put().uri("/assignments")
                .headers(h -> h.setBearerAuth(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateDto)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AssignmentDTO.class)
                .value(dto -> {
                    assertEquals("Updated Test Assignment", dto.title());
                    assertEquals("2025-10-02", dto.dueDate());
                });

        Assignment updated = assignmentRepository.findById(saved.id()).orElseThrow();
        assertEquals("Updated Test Assignment", updated.getTitle());

        // delete assignment
        client.delete().uri("/assignments/" + saved.id())
                .headers(h -> h.setBearerAuth(jwt))
                .exchange()
                .expectStatus().isOk();

        assertFalse(assignmentRepository.findById(saved.id()).isPresent());
    }

    @Test
    public void getAssignmentsForSection() throws Exception {
        // login as instructor
        String jwt = loginAndGetJwt("ted@csumb.edu", "ted2025");

        List<Section> sections = new ArrayList<>();
        sectionRepository.findAll().forEach(sections::add);
        assertFalse(sections.isEmpty());
        int sectionNo = sections.get(0).getSectionNo();

        client.get().uri("/sections/{secNo}/assignments", sectionNo)
                .headers(h -> h.setBearerAuth(jwt))
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(AssignmentDTO.class)
                .value(list -> assertNotNull(list));
    }

    @Test
    public void getStudentAssignments() throws Exception {
        // login as student
        String jwt = loginAndGetJwt("sam@csumb.edu", "sam2025");

        client.get().uri(uri -> uri.path("/assignments")
                        .queryParam("year", "2025")
                        .queryParam("semester", "Fall")
                        .build())
                .headers(h -> h.setBearerAuth(jwt))
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(AssignmentStudentDTO.class)
                .value(list -> assertNotNull(list));
    }
}


