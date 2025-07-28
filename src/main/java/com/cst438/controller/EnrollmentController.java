package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.EnrollmentDTO;
import com.cst438.service.RegistrarServiceProxy;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

import java.util.List;

@RestController
public class EnrollmentController {

    private final EnrollmentRepository enrollmentRepository;
    private final SectionRepository sectionRepository;
    private final RegistrarServiceProxy registrar;

    public EnrollmentController (
            EnrollmentRepository enrollmentRepository,
            SectionRepository sectionRepository,
            RegistrarServiceProxy registrar
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.sectionRepository = sectionRepository;
        this.registrar = registrar;
    }


    // instructor gets student enrollments with grades for a section
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    @GetMapping("/sections/{sectionNo}/enrollments")
    public List<EnrollmentDTO> getEnrollments(
            @PathVariable("sectionNo") int sectionNo, Principal principal ) {
				
		// check that the sectionNo belongs to the logged in instructor.
        Section section = sectionRepository.findById(sectionNo).orElse(null);
        if(section == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "section number not found");
        }
        if(!section.getInstructorEmail().equals(principal.getName())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Logged in user is not instructor for the given assignment");
        }
		// use the EnrollmentRepository findEnrollmentsBySectionNoOrderByStudentName

		// to get a list of Enrollments for the given sectionNo.

		// Return a list of EnrollmentDTOs
        Course course = section.getCourse();
        Term term = section.getTerm();
        return enrollmentRepository.findEnrollmentsBySectionNoOrderByStudentName(sectionNo).stream().map((e)-> {
                User student = e.getStudent();
                return new EnrollmentDTO(
                        e.getEnrollmentId(),
                        e.getGrade(),
                        student.getId(),
                        student.getName(),
                        student.getEmail(),
                        course.getCourseId(),
                        course.getTitle(),
                        section.getSectionId(),
                        sectionNo,
                        section.getBuilding(),
                        section.getRoom(),
                        section.getTimes(),
                        course.getCredits(),
                        term.getYear(),
                        term.getSemester()
                );
        }).toList();
    }

    // instructor updates enrollment grades
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    @PutMapping("/enrollments")
    public void updateEnrollmentGrade(@Valid @RequestBody List<EnrollmentDTO> dtoList, Principal principal) {
		// for each EnrollmentDTO
        for(EnrollmentDTO enrollmentDTO: dtoList){
            Enrollment enrollment = enrollmentRepository.findById(enrollmentDTO.enrollmentId()).orElse(null);
            if(enrollment == null){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "enrollment id not found");
            }
            //    check that logged in user is instructor for the section
            Section section = enrollment.getSection();
            if(!section.getInstructorEmail().equals(principal.getName())){
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Logged in user is not instructor for the given section");
            }
            //    update the enrollment grade
            enrollment.setGrade(enrollmentDTO.grade());
            enrollmentRepository.save(enrollment);
            //    send message to Registrar service for grade update

            registrar.sendMessage("updateEnrollment", enrollmentDTO);
        }
       
    }
}
