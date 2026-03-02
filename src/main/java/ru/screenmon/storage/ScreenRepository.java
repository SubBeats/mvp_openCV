package ru.screenmon.storage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScreenRepository extends JpaRepository<Screen, Long> {

    List<Screen> findAllByOrderByIdAsc();
}
