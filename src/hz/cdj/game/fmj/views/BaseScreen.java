package hz.cdj.game.fmj.views;

import org.apache.log4j.Logger;

import billy.StackPrintUtil;
import android.graphics.Canvas;

public abstract class BaseScreen {

    private static final Logger LOG = Logger.getLogger(BaseScreen.class);
    
	public BaseScreen() {
	    StackPrintUtil.p(this.getClass().getName());
	    LOG.debug("new " + this.getClass().getSimpleName() + "()");
    }

    public abstract void update(long delta);
	
	public abstract void draw(Canvas canvas);
	
	public abstract void onKeyDown(int key);
	
	public abstract void onKeyUp(int key);
	
	public boolean isPopup() {
		return false;
	}
}
