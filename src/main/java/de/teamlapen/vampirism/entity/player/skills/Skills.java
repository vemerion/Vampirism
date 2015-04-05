package de.teamlapen.vampirism.entity.player.skills;

import java.util.ArrayList;

import de.teamlapen.vampirism.util.Logger;

/**
 * Manages the vampire skills and their registration
 * 
 * @author maxanier
 *
 */
public class Skills {
	private final static ArrayList<ISkill> skills = new ArrayList<ISkill>();

	/**
	 * Used by {@link de.teamlapen.vampirism.client.gui.GUISelectSkill}
	 * 
	 * @return
	 */
	public static ArrayList<ISkill> getAvailableSkills(int level) {
		ArrayList<ISkill> sl = new ArrayList<ISkill>();
		for (ISkill s : skills) {
			if (level >= s.getMinLevel()&&s.getMinLevel()!=-1) {
				sl.add(s);
			}
		}
		return sl;
	}

	/**
	 * Returns the skill with the given id, might return null if the skill doesn't exist
	 * 
	 * @param i
	 * @return
	 */
	public static ISkill getSkill(int i) {
		try {
			return skills.get(i);
		} catch (IndexOutOfBoundsException e) {
			Logger.e("Skills", "Skill with id " + i + " doesn't exist");
			return null;
		}
	}

	public static int getSkillCount() {
		return skills.size();
	}

	/**
	 * Register all default skills
	 */
	public static void registerDefaultSkills() {
		VampireLordSkill.ID = Skills.registerSkill(new VampireLordSkill());
		Skills.registerSkill(new RegenSkill());
		Skills.registerSkill(new ChangeWeatherSkill());
	}

	/**
	 * 
	 * @param s
	 * @return The assigned id
	 */
	public static int registerSkill(ISkill s) {
		int id=skills.size();
		s.setId(id);
		skills.add(s);
		return id;
	}
}
