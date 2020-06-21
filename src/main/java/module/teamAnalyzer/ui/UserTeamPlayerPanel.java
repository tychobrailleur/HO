package module.teamAnalyzer.ui;

import core.gui.theme.HOColorName;
import core.gui.theme.HOIconName;
import core.gui.theme.ImageUtilities;
import core.gui.theme.ThemeManager;
import core.model.player.MatchRoleID;
import module.teamAnalyzer.report.TacticReport;
import module.teamAnalyzer.vo.UserTeamSpotLineup;

import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;

public class UserTeamPlayerPanel extends PlayerPanel {
    //~ Instance fields ----------------------------------------------------------------------------
	private static final long serialVersionUID = -7445084726999652737L;

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    protected Color getBackGround() {
        return ThemeManager.getColor(HOColorName.PANEL_BG);
    }

    @Override
	public Dimension getDefaultSize() {
        return new Dimension(150, 60);
    }

    public void reload(UserTeamSpotLineup lineup) {
        if (lineup != null) {
        	
        	containsPlayer = true;
        	
            nameField.setText(lineup.getName());
            positionImage.setIcon(ImageUtilities.getImage4Position(
                    lineup.getSpot(),
                    (byte) lineup.getTacticCode(),
                    0
            ));
            specialEventImage.setIcon(ThemeManager.getIcon(HOIconName.SPECIALTIES[lineup.getSpecialEvent()]));
            positionField.setText(MatchRoleID.getNameForPosition((byte) lineup.getPosition()));
            updateRatingPanel(lineup.getRating());
            tacticPanel.reload(new ArrayList<>());
        } else {
        	containsPlayer = false;
        	
            nameField.setText("");
            positionField.setText("");
            updateRatingPanel(0);
            positionImage.setIcon(ImageUtilities.getImage4Position(0, (byte) 0,0));
            specialEventImage.setIcon(null);
            tacticPanel.reload(new ArrayList<>());
        }
    }
}
