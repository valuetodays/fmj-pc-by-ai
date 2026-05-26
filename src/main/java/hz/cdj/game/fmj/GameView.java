package hz.cdj.game.fmj;

import hz.cdj.game.fmj.graphics.TextRender;
import hz.cdj.game.fmj.graphics.Util;
import hz.cdj.game.fmj.lib.DatLib;
import hz.cdj.game.fmj.scene.ScreenMainGame;
import hz.cdj.game.fmj.script.ScriptProcess;
import hz.cdj.game.fmj.views.BaseScreen;
import hz.cdj.game.fmj.views.ScreenAnimation;
import hz.cdj.game.fmj.views.ScreenMenu;
import hz.cdj.game.fmj.views.ScreenSaveLoadGame;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ListIterator;
import java.util.Stack;

import javax.swing.JFrame;

import org.apache.log4j.Logger;

import android.graphics.Canvas;

public class GameView extends JFrame implements Runnable{
	private static final long serialVersionUID = 4865220132498519554L;
	private static final Logger LOG = Logger.getLogger(GameView.class);

	private static GameView instance;
	private Stack<BaseScreen> mScreenStack;
	
	public Panel panel;
	
	
	Canvas canvas;
	
	/**
	 * 控制逻辑线程执行
	 */
	private  boolean mKeepRunning = true;

	public GameView() {
		LOG.debug("new " + this.getClass().getSimpleName() + "()");
		panel=new Panel();
		this.add(panel);//绘制类
		
		addL();//键盘监听
		
		canvas=new Canvas();
		instance = this;
	}
	
	public static void main(String[] args) {
		LOG.debug("game starts");
		//游戏窗口
		GameView gameView	=	new GameView();
		gameView.setTitle("伏魔记--PC.java版");
		gameView.setLocation(400, 200);
		gameView.setSize(Global.SCREEN_WIDTH*Global.Scale, Global.SCREEN_HEIGHT *Global.Scale);
		gameView.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		gameView.setVisible(true);
		gameView.setResizable(false);
		gameView.setAlwaysOnTop(true);
				
		//加载游戏
		initRes();
		gameView.mScreenStack = new Stack<BaseScreen>();
//		gameView.mScreenStack.push(new ScreenMenu()); 
		gameView.mScreenStack.push(new ScreenAnimation(249)); 
//		gameView.mScreenStack.push(new ScreenAnimation(248));
//		gameView.mScreenStack.push(new ScreenAnimation(247)); // 显示开发商动画
		
		gameView.mKeepRunning = true;
		new Thread(gameView, "logic update").start();
	}

	public synchronized static void initRes() {
		DatLib.init();
		TextRender.init();
		Util.init();
		ScriptProcess.init();
    }
	
	

	public static GameView getInstance() {
		return instance;
	}
	
	public void changeScreen(int screenCode) {
		BaseScreen tmp = null;
		switch (screenCode) {
		case Global.SCREEN_DEV_LOGO:
			tmp = new ScreenAnimation(247);
			break;
			
		case Global.SCREEN_GAME_LOGO:
			tmp = new ScreenAnimation(248);
			break;
			
		case Global.SCREEN_MENU:
			tmp = new ScreenMenu();
			break;
			
		case Global.SCREEN_MAIN_GAME:
			tmp = new ScreenMainGame();
			break;
			
		case Global.SCREEN_GAME_FAIL:
			tmp = new ScreenAnimation(249);
			break;
			
		case Global.SCREEN_SAVE_GAME:
			tmp = new ScreenSaveLoadGame(ScreenSaveLoadGame.Operate.SAVE);
			break;
			
		case Global.SCREEN_LOAD_GAME:
			tmp = new ScreenSaveLoadGame(ScreenSaveLoadGame.Operate.LOAD);
			break;
		}
		if (tmp != null) {
			mScreenStack.clear();
			mScreenStack.push(tmp);
		}
		System.gc();
	}
	
	public void pushScreen(BaseScreen screen) {
		mScreenStack.push(screen);
	}
	
	public void popScreen() {
		mScreenStack.pop();
	}
	
	public BaseScreen getCurScreen() {
		return mScreenStack.peek();
	}

	@Override
	public void run() {
		long curTime = System.currentTimeMillis();
		long lastTime = curTime;
		while (mKeepRunning) {
 			synchronized (mScreenStack) {
 				curTime = System.currentTimeMillis();
 				mScreenStack.peek().update(curTime - lastTime);
 				if (mScreenStack.size() > 1) {
 				    LOG.error("mScreenStack.size=" + mScreenStack.size());
 				}
 				lastTime = curTime;
				
				ListIterator<BaseScreen> iter = mScreenStack.listIterator(mScreenStack.size());
				// 找到第一个全屏窗口
				while (iter.hasPrevious()) {
					BaseScreen tmp = iter.previous();
					if (!tmp.isPopup()) {
						break;
					}
				}
				
				// 刷新
				Canvas canvas =GameView.instance.canvas;
				
				if (canvas != null) {
					while (iter.hasNext()) {
						iter.next().draw(canvas);
					}
					GameView.getInstance().panel.repaint();
				}
			}
			try {
				Thread.sleep(Global.TIME_GAMELOOP);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} // end of while()
	}

	public void keyDown(int key) {
		
		
		synchronized (mScreenStack) {
			mScreenStack.peek().onKeyDown(key);
		}
	}
	
	public void keyUp(int key) {
		synchronized (mScreenStack) {
			mScreenStack.peek().onKeyUp(key);
		}
	}
	
	public void addL() {
		this.addKeyListener(new KeyListener() {//键盘监听
			
			@Override
			public void keyTyped(KeyEvent e) {
				//System.out.println("1"+e.getKeyChar());
			}
			
			@Override
			public void keyReleased(KeyEvent e) {
				int key = c(e.getKeyCode());
				synchronized (mScreenStack) {
					mScreenStack.peek().onKeyUp(key);
				}
			}
			@Override
			public void keyPressed(KeyEvent e) {
				int key = c(e.getKeyCode());
				synchronized (mScreenStack) {
					mScreenStack.peek().onKeyDown(key);
				}
			}
			
			int c(int c){
				int key = -1;
				
				switch (c) {
				case KeyEvent.VK_LEFT:
				case KeyEvent.VK_A:
					key = Global.KEY_LEFT;
					break;
				case KeyEvent.VK_RIGHT:
				case KeyEvent.VK_D:
					key = Global.KEY_RIGHT;
					break;
				case KeyEvent.VK_UP:
				case KeyEvent.VK_W:
					key = Global.KEY_UP;
					break;
				case KeyEvent.VK_DOWN:
				case KeyEvent.VK_S:
					key = Global.KEY_DOWN;
					break;
				case KeyEvent.VK_NUMPAD3:
				case KeyEvent.VK_P:
					key = Global.KEY_PAGEDOWN;
					break;
				case KeyEvent.VK_NUMPAD6:
				case KeyEvent.VK_O:
					key = Global.KEY_PAGEUP;
					break;
				case KeyEvent.VK_NUMPAD1:
				case KeyEvent.VK_ENTER:
					key = Global.KEY_ENTER; 
					break;
				case KeyEvent.VK_NUMPAD2:
				case KeyEvent.VK_ESCAPE:
					key = Global.KEY_CANCEL; 
					break;
				}
				return key;
			}
		});
	}
	
	
	
	

}
