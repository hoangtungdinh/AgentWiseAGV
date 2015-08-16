package resourceAgents;

import com.google.common.collect.Range;

/**
 * The Class FreeTimeWindow.
 *
 * @author Tung
 */
public class FreeTimeWindow {
  
  /** The free time window. */
  private Range<Long> interval;
  
  /** The entry window. */
  private Range<Long> entryWindow;
  
  /** The exit window. */
  private Range<Long> exitWindow;

  /**
   * Instantiates a new free time window.
   *
   * @param interval the interval
   * @param entryWindow the entry window
   * @param exitWindow the exit window
   */
  public FreeTimeWindow(Range<Long> interval, Range<Long> entryWindow,
      Range<Long> exitWindow) {
    this.interval = interval;
    this.entryWindow = entryWindow;
    this.exitWindow = exitWindow;
  }

  /**
   * Gets the interval.
   *
   * @return the interval
   */
  public Range<Long> getInterval() {
    return interval;
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
