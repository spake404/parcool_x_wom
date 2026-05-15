package yesman.epicfight.skill;

public class SkillDataKeys {
	public static final Holder<SkillDataKey<Integer>> JUMP_COUNT = new Holder<>();
	public static final Holder<SkillDataKey<Boolean>> JUMP_KEY_PRESSED_LAST_TICK = new Holder<>();

	public static class Holder<T> {
		public T get() {
			return null;
		}
	}
}
