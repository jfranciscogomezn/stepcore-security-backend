package com.stepcore.security.repository;

import com.stepcore.security.domain.model.MenuNode;
import com.stepcore.security.domain.model.MenuNodeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MenuNodeRepository extends JpaRepository<MenuNode, Long> {

    List<MenuNode> findAllByOrderBySortOrderAsc();

    List<MenuNode> findAllByNodeTypeOrderBySortOrderAsc(MenuNodeType nodeType);

    /** Returns all nodes that are NOT platform-only; used for tenant-admin catalogue. */
    List<MenuNode> findByPlatformOnlyFalse();

    Optional<MenuNode> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByParentId(Long parentId);
}
