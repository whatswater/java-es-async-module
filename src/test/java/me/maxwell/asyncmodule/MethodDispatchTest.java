package me.maxwell.asyncmodule;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MethodDispatchTest {

    public static class Father {
        private int state;

        public Father(int state) {
            this.state = state;
        }

        public void addState() {
            state++;
        }

        public int getState() {
            addState();
            return state;
        }
    }

    public static class Son extends Father {
        private int state;

        public Son(int state, int sonState) {
            super(state);
            this.state = sonState;
        }

        public int getState() {
            return super.getState();
        }

        public void addState() {
            state++;
        }

        public int getSonState() {
            return state;
        }
    }

    @Test
    public void testMethodDispatch() {
        Son son = new Son(1, 3);
        Assertions.assertEquals(1, son.getState());
        Assertions.assertEquals(4, son.getSonState());
    }
}
