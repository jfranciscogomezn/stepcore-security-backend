package com.gmm.devengos.repository;

import com.gmm.devengos.domain.model.MenuOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuOptionRepository extends JpaRepository<MenuOption, Long> {

    List<MenuOption> findAllByOrderBySortOrderAsc();
}
