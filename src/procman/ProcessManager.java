package procman;

import java.io.File;
import java.util.Arrays;

/**
 * Lists, filters and manages system processes.
 */
public class ProcessManager {
  // Stores the OS file path separator character. Needs to escape to use as string splitter.
  private static final String FILE_PATH_SPLITTER = File.separator.replace("\\", "\\\\");

  private String pidFilter;
  private String pathFilter;
  private String nameFilter;
  private String userFilter;

  public ProcessManager() {
    clearFilters();
  }

  /**
   * Kills a single process.
   *
   * @param process Process to be killed.
   * @param forcibly If 'true' terminates the process immediately, otherwise allows the process to
   *        do a clean shut down.
   * @return 'true' if termination was successfully requested, otherwise 'false'.
   */
  public static boolean kill(ProcessHandle process, Boolean forcibly) {
    return process == null ? false : (forcibly ? process.destroyForcibly() : process.destroy());
  }

  /**
   * Kills a process and all of its descendants (children and descendants of the children).
   *
   * @param process Process to be killed alongside its descendants.
   * @param forcibly If 'true' terminates the process immediately, otherwise allows the process to
   *        do a clean shut down.
   * @return 'true' if termination of process and all of its descendants was successfully requested,
   *         otherwise 'false' if at least one (process or descendant) failed.
   */
  public static boolean killAll(ProcessHandle process, Boolean forcibly) {
    var success = false;

    if (process != null) {
      success = process.descendants().map(child -> kill(child, forcibly)).allMatch(x -> x);
      success &= kill(process, forcibly);
    }

    return success;
  }

  public void setPidFilter(String pid) {
    pidFilter = pid;
  }

  public void setPathFilter(String path) {
    pathFilter = path;
  }

  public void setNameFilter(String name) {
    nameFilter = name;
  }

  public void setUserFilter(String user) {
    userFilter = user;
  }

  public void clearFilters() {
    pidFilter = "";
    pathFilter = "";
    nameFilter = "";
    userFilter = "";
  }

  public String pidFilter() {
    return pidFilter;
  }

  public String pathFilter() {
    return pathFilter;
  }

  public String nameFilter() {
    return nameFilter;
  }

  public String usserFilter() {
    return userFilter;
  }

  /**
   * Returns an array of objects representing running process that matches the current filters.
   *
   * Each object is an array where the elements types and values are: - 0: Long, process PID. - 1:
   * String, path to the process parent folder. - 2: String, process executable name. - 3: String,
   * user of the process.
   *
   * @return Array of objects representing running process that matches the current filters.
   */
  public Object[][] getProcesses() {
    return ProcessHandle.allProcesses().map((p) -> {
      final var info = p.info();
      final var paths = info.command().orElse("").split(FILE_PATH_SPLITTER);

      final var pid = Long.valueOf(p.pid());
      final var path = String.join(File.separator, Arrays.copyOf(paths, paths.length - 1));
      final var process = paths[paths.length - 1];
      final var user = info.user().orElse("");

      return new Object[] {pid, path, process, user};
    }).filter(p -> (!((String) p[1]).isEmpty()) // If user has no privilege path is empty.
        && p[0].toString().contains(pidFilter)
        && p[1].toString().toLowerCase().contains(pathFilter)
        && p[2].toString().toLowerCase().contains(nameFilter)
        && p[3].toString().toLowerCase().contains(userFilter)).toArray(Object[][]::new);
  }
}
