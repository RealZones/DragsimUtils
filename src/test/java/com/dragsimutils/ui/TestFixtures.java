package com.dragsimutils.ui;

import java.util.List;

/** In-memory controls used only by automated UI checks. */
final class TestFixtures {
    private TestFixtures() { }

    static Setting.Toggle toggle(String label, String description, boolean initial) {
        boolean[] v = {initial};
        return new Setting.Toggle(label, description, () -> v[0], x -> v[0] = x);
    }

    static Setting.Slider slider(String label, String description,
                                     double min, double max, double step,
                                     double initial, String unit) {
        double[] v = {initial};
        return new Setting.Slider(label, description, min, max, step, () -> v[0], x -> v[0] = x, unit);
    }

    static Setting.Choice choice(String label, String description,
                                     List<String> options, String initial) {
        String[] v = {initial};
        return new Setting.Choice(label, description, options, () -> v[0], x -> v[0] = x);
    }

    static Module module(String id, String title, String description, String category) {
        return module(id, title, description, category, List.of());
    }

    static Module module(String id, String title, String description, String category, List<Setting> settings) {
        boolean[] enabled = {false};
        return new Module(id, title, description, category, () -> enabled[0], value -> enabled[0] = value, settings);
    }
}
