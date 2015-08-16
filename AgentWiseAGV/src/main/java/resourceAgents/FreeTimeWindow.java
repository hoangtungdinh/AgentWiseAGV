package resourceAgents;

import com.google.common.collect.Range;

/**
 * The Class FreeTimeWindow.
 *
 * @author Tung
 */
public class FreeTimeWindow {
  
  /** The free time window. */
  private Range<Long> freeTimeWindow;
  
  /** The entry window. */
  private Range<Long> entryWindow;
  
  /** The exit window. */
  private Range<Long> exitWindow;

  /**
   * Instantiates a new free time window.
   *
   * @param freeTimeWindow the free time window
   * @param entryWindow the entry window
   * @param exitWindow the exit window
   */
  public FreeTimeWindow(Range<Long> freeTimeWindow, Range<Long> entryWindow,
      Range<Long> exitWindow) {
    this.freeTimeWindow = freeTimeWindow;
    this.entryWindow = entryWindow;
    this.exitWindow = exitWindow;
  }

  /**
   * Gets the free time window.
   *
   * @return the free time window
   */
  public Range<Long> getFreeTimeWindow() {
    return freeTimeWindow;
  }

  /**
   * Gets the entry window.
   *
   * @return the entry window
   */
  public Range<Long> getEntryWindow() {
    return entryWindow;
  }

  /**
   * Gets the exit window.
   *
   * @return the exit window
   */
  public Range<Long> getExitWindow() {
    return exitWindow;
  }
 
  
}
