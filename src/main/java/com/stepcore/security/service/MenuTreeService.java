package com.stepcore.security.service;

import com.stepcore.security.controller.dto.role.MenuTreeNode;
import com.stepcore.security.domain.model.MenuNode;
import com.stepcore.security.domain.model.MenuNodeType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MenuTreeService {

    public List<MenuTreeNode> buildTree(final List<MenuNode> assignedItems, final List<MenuNode> catalogue) {
        final Map<Long, MenuNode> catalogueById = catalogue.stream()
                .collect(Collectors.toMap(MenuNode::getId, node -> node));

        final Set<Long> visibleIds = new HashSet<>();
        for (final MenuNode item : assignedItems) {
            if (!item.isEnabled() || item.getNodeType() != MenuNodeType.ITEM) {
                continue;
            }
            Long currentId = item.getId();
            while (currentId != null) {
                if (!visibleIds.add(currentId)) {
                    break;
                }
                final MenuNode current = catalogueById.get(currentId);
                currentId = current == null ? null : current.getParentId();
            }
        }

        final Map<Long, MenuNode> visibleById = visibleIds.stream()
                .filter(catalogueById::containsKey)
                .collect(Collectors.toMap(id -> id, catalogueById::get));

        return buildChildren(null, visibleById);
    }

    public List<String> extractPermissions(final List<MenuNode> assignedItems) {
        return assignedItems.stream()
                .filter(node -> node.getNodeType() == MenuNodeType.ITEM && node.isEnabled())
                .map(MenuNode::getCode)
                .sorted()
                .toList();
    }

    public List<MenuTreeNode> buildCatalogueTree(final List<MenuNode> catalogue) {
        return buildCatalogueTree(catalogue, false);
    }

    public List<MenuTreeNode> buildCatalogueTree(final List<MenuNode> catalogue, final boolean includeDisabled) {
        final Map<Long, MenuNode> byId = catalogue.stream()
                .filter(node -> includeDisabled || node.isEnabled())
                .collect(Collectors.toMap(MenuNode::getId, node -> node, (a, b) -> a, HashMap::new));
        return buildChildren(null, byId);
    }

    private List<MenuTreeNode> buildChildren(final Long parentId, final Map<Long, MenuNode> visibleById) {
        final List<MenuTreeNode> result = new ArrayList<>();
        visibleById.values().stream()
                .filter(node -> matchesParent(node.getParentId(), parentId))
                .sorted(Comparator.comparingInt(MenuNode::getSortOrder))
                .forEach(node -> {
                    final List<MenuTreeNode> children = buildChildren(node.getId(), visibleById);
                    if (node.getNodeType() == MenuNodeType.ITEM || !children.isEmpty()) {
                        result.add(toDto(node, children));
                    }
                });
        return result;
    }

    private static boolean matchesParent(final Long nodeParentId, final Long expectedParentId) {
        if (expectedParentId == null) {
            return nodeParentId == null;
        }
        return expectedParentId.equals(nodeParentId);
    }

    private static MenuTreeNode toDto(final MenuNode node, final List<MenuTreeNode> children) {
        return new MenuTreeNode(
                node.getId(),
                node.getCode(),
                node.getLabel(),
                node.getNodeType(),
                node.getRoute(),
                node.getIcon(),
                node.isEnabled(),
                children);
    }
}
