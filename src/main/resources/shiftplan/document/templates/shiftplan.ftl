<#setting locale="de_DE">
<#setting date_format="dd.MM.yyyy">
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Schichtplan Luftfracht Export</title>
    <style>
        @page {
            size: a4 landscape;

            @bottom-center {
                content: 'Seite ' counter(page) ' von ' counter(pages);
            }
        }
        @media print {
            html, body {
                height: 99%;
            }
            h1 {
                font-size: 10pt;
            }
            #shiftplan, #homeoffice-control {
                font-family: serif;
                font-size: 9pt;
            }
        }

        html,
        body {
            margin: 0;
            padding: 0;
            /*height: auto;*/
            font-size: 1.0em;
            background-color: #fff;
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
            font-size: small;
        }

        #employee-details {
            font-size: x-small;
            float: left;
        }

        #employee-details th {
            text-align: left;
            padding: 3px;
        }

        #shift-info {
            margin-left: 100px;
            font-size: x-small;
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
            -fs-table-paginate: paginate;
            page-break-inside: auto;
        }

        #shiftplan thead {
            display: table-header-group;
        }

        #shiftplan tr {
            page-break-inside: avoid;
            page-break-after: auto;
        }

        #shiftplan > thead th {
            padding: 5px;
            border: 1px solid black;
        }

        #shiftplan .sub-header {
            font-weight: normal;
            font-size: xx-small;
        }

        #shiftplan > tbody tr:nth-child(odd) {
            background-color: #fff;
        }

        #shiftplan > tbody tr:nth-child(even) {
            background-color: #ddd;
        }

        #shiftplan > tbody th {
            padding: 5px;
            font-size: 0.9em;
            font-weight: normal;
            border: 1px solid black;
        }

        #shiftplan > tbody td {
            padding: 5px;
            font-size: 0.8em;
            border: 1px solid black;
        }

        #shiftplan > tbody td > ul {
            margin: 0 0 0 5px;
            padding-left: 0;
            list-style-type: square;
        }

        #shiftplan .small {
            font-size: xx-small;
        }

        #homeoffice-control-section {
            margin-top: 100px;
        }

        #homeoffice-control caption {
            page-break-before: always;
            margin-bottom: 5px;
            border-bottom: 1px solid silver;
            font-style: italic;
        }

        #homeoffice-control {
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
    </style>
</head>
<body>
<header>
    <h1>Schichtplan Luftfracht Export vom ${startDate.format("dd.MM.yyyy")} bis ${endDate.format("dd.MM.yyyy")}</h1>
</header>
<main>
    <section class="legend">
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
                <td class="left">Anzahl Homeoffice-Slots:</td>
                <td>${shiftInfo["hoSlotsPerShift"]} Slots pro Tag</td>
            </tr>
            <tr>
                <td class="left">Anzahl Homeoffice-Tage:</td>
                <td>${shiftInfo["hoCreditsPerWeek"]} Tage pro MA / Woche</td>
            </tr>
            <tr>
                <td class="left">Max. Anzahl von HO-Tagen:</td>
                <td>${shiftInfo["maxHoDaysPerMonth"]} Tage pro Monat</td>
            </tr>
            </tbody>
        </table>
        <div class="clear-float"></div>
    </section>
    <section>
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
    <section id="homeoffice-control-section">
        <table id="homeoffice-control">
            <caption>
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
    </section>
</main>
</body>
</html>