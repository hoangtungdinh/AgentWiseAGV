package resourceagents;

import com.google.common.base.Objects;
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
 
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return ("\nInterval: " + interval + "\nEntry window: " + entryWindow
        + "\nExit window: " + exitWindow + "\n");
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }
    if (other == this) {
      return true;
    }
    // allows comparison with subclasses
    if (!(other instanceof FreeTimeWindow)) {
      return false;
    }

    final FreeTimeWindow ftw = (FreeTimeWindow) other;
    return this.interval.equals(ftw.getInterval())
        && this.entryWindow.equals(ftw.getEntryWindow())
        && this.exitWindow.equals(ftw.exitWindow);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(interval, entryWindow, exitWindow);
  }
}
