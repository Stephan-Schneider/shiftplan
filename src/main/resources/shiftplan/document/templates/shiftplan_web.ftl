<#setting locale="de_DE">
<#setting date_format="dd.MM.yyyy">
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Schichtplan Luftfracht Export</title>

    <script>
        let navLinks;

        function init() {
            navLinks = document.getElementsByTagName("a");

            for (let link of navLinks) {
                link.addEventListener("click", toggleDisplay);
            }
        }


        function toggleDisplay(evt) {
            evt.preventDefault();
            // Link-Id's (<a id="...">): link-legend|shiftplan|homeoffice
            // Section-Id's (section id="..."): legend|shiftplan|homeoffice-section
            const clickedLinkId = evt.currentTarget.id;
            const sectionId = clickedLinkId.substring(clickedLinkId.indexOf("-") +1) + "-section";
            const targetElement = document.getElementById(sectionId)
            const currentDisplayState = window.getComputedStyle(targetElement).getPropertyValue("display");
            if (currentDisplayState === "none") {
                targetElement.style.display = "block";
            } else {
                targetElement.style.display = "none";
            }
        }

        document.addEventListener("DOMContentLoaded", init);
    </script>

    <style>
        html,
        body {
            margin: 0;
            padding: 0;
            /*height: auto;*/
            font-size: 1.0em;
            background-color: #fff;
        }

        nav {
            margin: 0 0 0 100px;
        }

        nav a:link {
            display: inline-block;
            color: black;
            border: 1px solid black;
            padding: 3px;
            width: 150px;
            text-decoration: none;
        }

        nav a:hover {
            text-decoration: underline;
        }

        nav a:visited {
            color: black;
            border: 1px solid black;
            text-decoration: none;
        }

        #legend-section {
            display: none;
        }

        #shiftplan-section {
            display: none;
        }

        #homeoffice-section {
            display: none;
        }


        main {
            margin: 50px 100px 50px 100px;
            height: auto;
        }

        header {
            margin: 50px 100px 0 100px;
        }

        section.legend {
            margin-bottom: 50px;
        }

        section.legend > h2 {
            font-size: 1.1em;
        }

        #employee-details {
            float: left;
        }

        #employee-details th {
            text-align: left;
            padding: 3px;
        }

        #shift-info {
            margin-left: 100px;
            float: left;
        }

        #shift-info td.left {
            font-weight: bold;
        }

        div.clear-float {
            clear: left;
        }

        #shiftplan {
            width: 100%;
            border: 1px solid black;
            border-collapse: collapse;
        }

        #shiftplan thead {
            display: table-header-group;
        }

        /*#shiftplan tr {
            page-break-inside: avoid;
            page-break-after: auto;
        }*/

        #shiftplan > thead th {
            padding: 5px;
            border: 1px solid black;
        }

        #shiftplan .sub-header {
            font-weight: normal;
            font-size: 1.1em;
        }

        #shiftplan > tbody tr:nth-child(odd) {
            background-color: #fff;
        }

        #shiftplan > tbody tr:nth-child(even) {
            background-color: #ddd;
        }

        #shiftplan > tbody th {
            padding: 5px;
            /*font-size: 0.9em;*/
            font-weight: normal;
            border: 1px solid black;
        }

        #shiftplan > tbody td {
            padding: 5px;
            font-size: 1.1em;
            border: 1px solid black;
        }

        #shiftplan > tbody td > ul {
            margin: 0 0 0 10px;
            padding-left: 5px;
            list-style-type: square;
        }

        #shiftplan .small {
            font-size: small;
        }

        section.homeoffice-control-section {
            margin-top: 100px;
        }

        .homeoffice-control-caption {
            page-break-before: always;
            margin-bottom: 5px;
            border-bottom: 1px solid silver;
            font-style: italic;
        }

        #homeoffice-control,#undistributedHo {
            width: 50%;
            margin: 15px auto 0 auto;
            border: 1px solid black;
            border-collapse: collapse;
            page-break-inside: auto;
        }

        #homeoffice-control thead {
            display: table-header-group;
        }

        #homeoffice-control tr {
            page-break-inside: avoid;
            page-break-after: auto;
        }

        #homeoffice-control > thead th {
            padding: 5px;
            border: 1px solid black;
        }

        #homeoffice-control > tbody tr:nth-child(odd) {
            background-color: #fff;
        }

        #homeoffice-control > tbody tr:nth-child(even) {
            background-color: #ddd;
        }

        #homeoffice-control > tbody th {
            padding: 5px;
            font-size: 0.9em;
            font-weight: normal;
            border: 1px solid black;
        }

        #homeoffice-control > tbody td {
            padding: 5px;
            font-size: 0.8em;
            border: 1px solid black;
        }

        #undistributedHo thead {
            display: table-header-group;
        }

        #undistributedHo tr {
            page-break-inside: avoid;
            page-break-after: auto;
        }

        #undistributedHo thead th {
            padding: 5px;
            font-size: 0.8em;
            font-weight: normal;
            border: 1px solid black;
        }

        /*#undistributedHo > tbody th {
            padding: 5px;
            font-size: 0.9em;
            font-weight: normal;
            border: 1px solid black;
        }*/

        #undistributedHo tbody td {
            padding: 5px;
            font-size: 0.8em;
            border: 1px solid black;
        }
    </style>
</head>
<body>
<header>
    <h1>Schichtplan Luftfracht Export vom ${startDate.format("dd.MM.yyyy")} bis ${endDate.format("dd.MM.yyyy")}</h1>
</header>
<nav>
    <a href="#" id="link-legend" class="nav-link">Legende</a>
    <a href="#" id="link-shiftplan" class="nav-link">Schichtplan</a>
    <a href="#" id="link-homeoffice" class="nav-link">HO-Zuweisungen</a>
</nav>
<main>
    <section id="legend-section" class="legend">
        <h2>Legende</h2>
        <table id="employee-details">
            <thead>
            <tr>
                <th>Mitarbeiter</th>
                <th>Teilnahme</th>
                <th>Backups</th>
            </tr>
            </thead>
            <tbody>
            <#list employees as employee>
                <#assign participation_schema>
                    <#switch employee.participationSchema.name()>
                        <#case "HO">
                            H
                            <#break>
                        <#case "LS">
                            S
                            <#break>
                        <#case "HO_LS">
                            H+S
                            <#break>
                    </#switch>
                </#assign>
            <tr>
                <td style="color: ${employee.highlightColor};">${employee.name} ${employee.lastName}</td>
                <td>${participation_schema}</td>
                <td><#list employee.backups as backup>${backup.name} ${backup.lastName}<#sep>,</#list></td>
            </tr>
            </#list>
            </tbody>
        </table>
        <table id="shift-info">
            <tbody>
            <tr>
                <td class="left">Dauer Spätschicht-Zyklus:</td>
                <td>${shiftInfo["lateShiftDuration"]} Tage</td>
            </tr>
            <tr>
                <td class="left">Anzahl Homeoffice-Stellen:</td>
                <td>${shiftInfo["hoSlotsPerShift"]} MA's pro Tag</td>
            </tr>
            <tr>
                <td class="left">Anzahl Homeoffice-Tage:</td>
                <td>${shiftInfo["hoCreditsPerWeek"]} Tage pro MA / Woche</td>
            </tr>
            <tr>
                <td class="left">Max. Anzahl von HO-Tagen:</td>
                <td>${shiftInfo["maxHoDaysPerMonth"]} Tage pro Monat</td>
            </tr>
            <tr>
                <td class="left">Max. Anzahl aufeinanderfolgender HO-Tage:</td>
                <td>${shiftInfo["maxSuccessiveHODays"]} Tage</td>
            </tr>
            <tr>
                <td class="left">Mindestdauer der HO-Sperre nach ${shiftInfo["maxSuccessiveHODays"]} Tagen:</td>
                <td>${shiftInfo["minDistanceBetweenHOBlocks"]} Tage</td>
            </tr>
            </tbody>
        </table>
        <div class="clear-float"></div>
    </section>
    <section id="shiftplan-section">
        <table id="shiftplan">
            <thead>
            <tr>
                <th>KW</th>
                <th>Von -> Bis</th>
                <th colspan="2">Montag</th>
                <th colspan="2">Dienstag</th>
                <th colspan="2">Mittwoch</th>
                <th colspan="2">Donnerstag</th>
                <th colspan="2">Freitag</th>
            </tr>
            <tr>
                <th></th>
                <th></th>
                <th class="sub-header">Home Office</th>
                <th class="sub-header">Spätdienst</th>
                <th class="sub-header">Home Office</th>
                <th class="sub-header">Spätdienst</th>
                <th class="sub-header">Home Office</th>
                <th class="sub-header">Spätdienst</th>
                <th class="sub-header">Home Office</th>
                <th class="sub-header">Spätdienst</th>
                <th class="sub-header">Home Office</th>
                <th class="sub-header">Spätdienst</th>
            </tr>
            </thead>
            <tbody>
            <#list calendar as cwIndex, calendarWeek>
                <tr>
                    <td>${cwIndex}</td>
                    <td>${calendarWeek[0].format("dd.MM.")} - ${calendarWeek[6].format("dd.MM.")}</td>
                    <#list calendarWeek as workday>
                        <#if workday?index gt 4>
                            <#continue>
                        </#if>
                        <#if shiftPlan[workday]??>
                            <td>
                                <#list shiftPlan[workday].employeesInHo>
                                    <ul>
                                        <#items as employee>
                                            <#if employee??>
                                                <li><span style="color: ${employee.highlightColor}">${employee.name}</span></li>
                                            <#else>
                                                <span class="small">Nicht besetzt</span><br>
                                            </#if>
                                        </#items>
                                    </ul>
                                    <#else>
                                        <p><span class="small">Nicht besetzt</span></p>
                                </#list>
                            </td>
                            <td
                                <#if shiftPlan[workday].lateShift??>
                                    style="color: ${shiftPlan[workday].lateShift.highlightColor}">
                                    ${shiftPlan[workday].lateShift.name}
                                <#else>
                                    >
                                    <span class="small">Keine Spätschicht</span>
                                </#if>
                            </td>
                        <#else>
                            <td colspan="2">Kein Arbeitstag</td>
                        </#if>
                    </#list>
                </tr>
            </#list>
            </tbody>
        </table>
    </section>
    <section id="homeoffice-section" class="homeoffice-control-section">
        <#if homeOfficeRecords??>
        <table id="homeoffice-control">
            <caption class="homeoffice-control-caption">
                <h4>Evaluierung der Homeoffice-Zuteilungen vom ${startDate.format("dd.MM.yyyy")} bis ${endDate.format("dd.MM.yyyy")}</h4>
            </caption>
            <thead>
                <tr>
                    <th>Monat</th>
                    <th>Name</th>
                    <th>HO-Tage im Plan</th>
                    <th>Nicht zugewiesene HO-Tage</th>
                </tr>
            </thead>
            <tbody>
                <#list homeOfficeRecords as record>
                    <tr>
                        <td>${record.monthName}</td>
                        <td>${record.employeeName}</td>
                        <td <#if record.notAssigned == 0>style="color: green"</#if>>
                            <#if record.optionsInPlan == 1>
                                ${record.optionsInPlan} ${"Tage"?keep_before("e")}
                            <#else>
                                ${record.optionsInPlan} Tage
                            </#if>
                        </td>
                        <td <#if record.notAssigned == 0>style="color: green"</#if>>
                            <#if record.notAssigned == 1>
                                ${record.notAssigned} ${"Tage"?keep_before("e")}
                            <#else>
                                ${record.notAssigned} Tage
                            </#if>
                        </td>
                    </tr>
                </#list>
            </tbody>
        </table>
        <#elseif swapResult?? && swapResult.swapHO>
        <table id="undistributedHo">
            <caption class="homeoffice-control-caption">
                <h4>Durch Schichtwechsel bedingte Reduzierung der Homeoffice-Tage (${swapResult.swapMode.name()})</h4>
            </caption>
            <thead>
                <tr>
                    <th>Mitarbeiter/in 1</th>
                    <th>HO-Tage reduziert</th>
                    <th>Mitarbeiter/in 2</th>
                    <th>HO-Tage reduziert</th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td>${swapResult.employeeA.name}</td>
                    <td>${swapResult.undistributedHoA}</td>
                    <td>${swapResult.employeeB.name}</td>
                    <td>${swapResult.undistributedHoB}</td>
                </tr>
            </tbody>
        </table>
        </#if>
    </section>
</main>
</body>
</html>