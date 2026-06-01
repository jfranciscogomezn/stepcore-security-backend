package com.stepcore.security.service;

import com.stepcore.security.controller.dto.menu.CreateMenuNodeRequest;
import com.stepcore.security.controller.dto.menu.MenuNodeAdminResponse;
import com.stepcore.security.controller.dto.menu.UpdateMenuNodeRequest;
import com.stepcore.security.controller.dto.role.MenuTreeNode;
import com.stepcore.security.domain.model.MenuNode;
import com.stepcore.security.domain.model.MenuNodeType;
import com.stepcore.security.exception.MenuNodeInUseException;
import com.stepcore.security.exception.MenuNodeNotFoundException;
import com.stepcore.security.exception.MenuNodeValidationException;
import com.stepcore.security.repository.MenuNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MenuServiceImpl implements MenuService {

    private final MenuNodeRepository menuNodeRepository;
    private final MenuTreeService menuTreeService;

    @Override
    @Transactional(readOnly = true)
    public List<MenuTreeNode> getCatalogue(final boolean includeDisabled) {
        return menuTreeService.buildCatalogueTree(menuNodeRepository.findAll(), includeDisabled);
    }

    @Override
    public MenuNodeAdminResponse create(final CreateMenuNodeRequest request) {
        if (menuNodeRepository.existsByCode(request.code())) {
            throw new MenuNodeValidationException("Menu node code already exists: " + request.code());
        }
        validateStructure(request.nodeType(), request.parentId(), request.route(), null);

        final MenuNode node = MenuNode.builder()
                .withCode(request.code())
                .withLabel(request.label())
                .withNodeType(request.nodeType())
                .withRoute(normalizeRoute(request.nodeType(), request.route()))
                .withIcon(request.icon())
                .withParentId(request.parentId())
                .withSortOrder(request.sortOrder())
                .withEnabled(request.enabled())
                .build();

        final MenuNode saved = menuNodeRepository.save(node);
        log.info("[MenuServiceImpl] - CREATE_MENU_NODE: id={} code={}", saved.getId(), saved.getCode());
        return toAdminResponse(saved);
    }

    @Override
    public MenuNodeAdminResponse update(final Long id, final UpdateMenuNodeRequest request) {
        final MenuNode node = menuNodeRepository.findById(id)
                .orElseThrow(() -> new MenuNodeNotFoundException(id));
        validateStructure(node.getNodeType(), node.getParentId(), request.route(), node.getId());

        node.updateDetails(
                request.label(),
                normalizeRoute(node.getNodeType(), request.route()),
                request.icon(),
                request.sortOrder(),
                request.enabled());

        final MenuNode saved = menuNodeRepository.save(node);
        log.info("[MenuServiceImpl] - UPDATE_MENU_NODE: id={} code={}", saved.getId(), saved.getCode());
        return toAdminResponse(saved);
    }

    @Override
    public void delete(final Long id) {
        final MenuNode node = menuNodeRepository.findById(id)
                .orElseThrow(() -> new MenuNodeNotFoundException(id));

        if (menuNodeRepository.existsByParentId(id)) {
            throw new MenuNodeInUseException("Cannot delete menu node with child nodes");
        }
        if (!node.getRoles().isEmpty()) {
            throw new MenuNodeInUseException("Cannot delete menu node assigned to one or more roles");
        }

        menuNodeRepository.delete(node);
        log.info("[MenuServiceImpl] - DELETE_MENU_NODE: id={} code={}", id, node.getCode());
    }

    private void validateStructure(
            final MenuNodeType nodeType,
            final Long parentId,
            final String route,
            final Long currentId) {
        switch (nodeType) {
            case MODULE -> {
                if (parentId != null) {
                    throw new MenuNodeValidationException("MODULE nodes must not have a parent");
                }
                if (route != null && !route.isBlank()) {
                    throw new MenuNodeValidationException("MODULE nodes must not have a route");
                }
            }
            case GROUP -> {
                if (parentId == null) {
                    throw new MenuNodeValidationException("GROUP nodes require a parent");
                }
                final MenuNode parent = loadParent(parentId);
                if (parent.getNodeType() == MenuNodeType.ITEM) {
                    throw new MenuNodeValidationException("GROUP parent must be a MODULE or GROUP");
                }
                if (route != null && !route.isBlank()) {
                    throw new MenuNodeValidationException("GROUP nodes must not have a route");
                }
            }
            case ITEM -> {
                if (parentId == null) {
                    throw new MenuNodeValidationException("ITEM nodes require a parent");
                }
                final MenuNode parent = loadParent(parentId);
                if (parent.getNodeType() == MenuNodeType.ITEM) {
                    throw new MenuNodeValidationException("ITEM parent must be a MODULE or GROUP");
                }
                if (route == null || route.isBlank()) {
                    throw new MenuNodeValidationException("ITEM nodes require a route");
                }
            }
        }
        if (currentId != null && parentId != null && currentId.equals(parentId)) {
            throw new MenuNodeValidationException("Menu node cannot be its own parent");
        }
    }

    private MenuNode loadParent(final Long parentId) {
        return menuNodeRepository.findById(parentId)
                .orElseThrow(() -> new MenuNodeValidationException("Parent menu node not found: " + parentId));
    }

    private static String normalizeRoute(final MenuNodeType nodeType, final String route) {
        if (nodeType == MenuNodeType.ITEM) {
            return route;
        }
        return null;
    }

    private static MenuNodeAdminResponse toAdminResponse(final MenuNode node) {
        return new MenuNodeAdminResponse(
                node.getId(),
                node.getCode(),
                node.getLabel(),
                node.getNodeType(),
                node.getRoute(),
                node.getIcon(),
                node.getParentId(),
                node.getSortOrder(),
                node.isEnabled());
    }
}
