package com.stepcore.security.service;

import com.stepcore.security.controller.dto.menu.CreateMenuNodeRequest;
import com.stepcore.security.controller.dto.menu.UpdateMenuNodeRequest;
import com.stepcore.security.domain.model.MenuNode;
import com.stepcore.security.domain.model.MenuNodeType;
import com.stepcore.security.exception.MenuNodeInUseException;
import com.stepcore.security.exception.MenuNodeNotFoundException;
import com.stepcore.security.exception.MenuNodeValidationException;
import com.stepcore.security.repository.MenuNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Mock private MenuNodeRepository menuNodeRepository;
    @Mock private MenuTreeService menuTreeService;

    @InjectMocks private MenuServiceImpl menuService;

    private MenuNode payrollModule;

    @BeforeEach
    void setUp() {
        payrollModule = MenuNode.builder()
                .withId(1L)
                .withCode("PAYROLL")
                .withLabel("Payroll")
                .withNodeType(MenuNodeType.MODULE)
                .withSortOrder(10)
                .withEnabled(true)
                .build();
    }

    @Test
    void shouldCreateItemMenuNode() {
        when(menuNodeRepository.existsByCode("NEW_SCREEN")).thenReturn(false);
        when(menuNodeRepository.findById(2L)).thenReturn(Optional.of(
                MenuNode.builder().withId(2L).withCode("GRP").withLabel("Group")
                        .withNodeType(MenuNodeType.GROUP).withParentId(1L).withSortOrder(10).build()));
        when(menuNodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        final var response = menuService.create(new CreateMenuNodeRequest(
                "NEW_SCREEN", "New Screen", MenuNodeType.ITEM, "/new", null, 2L, 10, true));

        assertThat(response.code()).isEqualTo("NEW_SCREEN");
        verify(menuNodeRepository).save(any());
    }

    @Test
    void shouldRejectModuleWithRoute() {
        assertThatThrownBy(() -> menuService.create(new CreateMenuNodeRequest(
                "BAD_MOD", "Bad", MenuNodeType.MODULE, "/bad", null, null, 10, true)))
                .isInstanceOf(MenuNodeValidationException.class);
    }

    @Test
    void shouldRejectDeleteWhenNodeHasChildren() {
        when(menuNodeRepository.findById(1L)).thenReturn(Optional.of(payrollModule));
        when(menuNodeRepository.existsByParentId(1L)).thenReturn(true);

        assertThatThrownBy(() -> menuService.delete(1L))
                .isInstanceOf(MenuNodeInUseException.class);
    }

    @Test
    void shouldUpdateMenuNodeLabel() {
        final MenuNode item = MenuNode.builder()
                .withId(5L).withCode("MY_SCREEN").withLabel("Old")
                .withNodeType(MenuNodeType.ITEM).withRoute("/old").withParentId(2L)
                .withSortOrder(10).withEnabled(true).build();
        when(menuNodeRepository.findById(5L)).thenReturn(Optional.of(item));
        when(menuNodeRepository.findById(2L)).thenReturn(Optional.of(
                MenuNode.builder().withId(2L).withCode("GRP").withLabel("Group")
                        .withNodeType(MenuNodeType.GROUP).withParentId(1L).withSortOrder(10).build()));
        when(menuNodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        final var response = menuService.update(5L, new UpdateMenuNodeRequest(
                "New Label", "/old", null, 20, true));

        assertThat(response.label()).isEqualTo("New Label");
        assertThat(response.sortOrder()).isEqualTo(20);
    }

    @Test
    void shouldThrowWhenMenuNodeNotFound() {
        when(menuNodeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> menuService.update(99L, new UpdateMenuNodeRequest(
                "X", "/x", null, 1, true)))
                .isInstanceOf(MenuNodeNotFoundException.class);
    }
}
