package org.keybinder.wurm.catalog;

import com.wurmonline.shared.constants.PlayerAction;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PlayerActionCatalog {
    @SuppressWarnings("unchecked")
    public List<PlayerAction> snapshot() throws ReflectiveOperationException {
        Field field = PlayerAction.class.getDeclaredField("actionIds");
        Map<Short, PlayerAction> source = ReflectionUtil.getPrivateField(null, field);
        Map<Short, PlayerAction> unique = new LinkedHashMap<>(source);
        List<PlayerAction> actions = new ArrayList<>(unique.values());
        actions.removeIf(x -> x == null);
        actions.sort(Comparator.comparing(
                        (PlayerAction action) -> action.getName() == null ? "" : action.getName(),
                        String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(PlayerAction::getId));
        return actions;
    }
}
