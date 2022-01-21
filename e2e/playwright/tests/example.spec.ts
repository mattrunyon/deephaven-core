import { test, expect } from "@playwright/test";

test("basic test", async ({ page }) => {
  await page.goto("/");

  await expect(page).toHaveTitle("Deephaven");

  const consoleInput = page.locator(".console-input");

  // Click .console-input
  await consoleInput.click();

  // Read csv example data
  await consoleInput.type("from deephaven import read_csv");
  await consoleInput.press("Shift+Enter");
  await consoleInput.type(
    'source = read_csv("https://media.githubusercontent.com/media/deephaven/examples/main/MetricCentury/csv/metriccentury.csv")'
  );
  await consoleInput.press("Enter");

  // Console input should clear
  await expect(consoleInput.locator("textarea")).toHaveText("");

  // Now click on the grid that was created
  const grid = page.locator(".iris-grid-panel canvas");
  await grid.click();

  expect(await grid.screenshot()).toMatchSnapshot("iris-grid-source.png");

  // Make sure the title is correct
  await expect(
    page.locator(".lm_tab.lm_active.lm_focusin .lm_title")
  ).toHaveText("source");

  // Now create a plot
  // Click .console-input
  await consoleInput.click();

  // Enter commands to create a plot from the previous data
  await consoleInput.type("from deephaven import Plot");
  await consoleInput.press("Shift+Enter");
  await consoleInput.type(
    'plot_single = Plot.plot("Distance",  source.where("SpeedKPH > 0"), "Time", "DistanceMeters").show()'
  );
  await consoleInput.press("Enter");

  // Wait for the plot lines to appear
  const plot = page.locator(".iris-chart-panel .cartesianlayer");
  await plot.locator(".scatterlayer .trace.scatter").waitFor();

  expect(await plot.screenshot()).toMatchSnapshot("iris-chart-plot_single.png");
});
