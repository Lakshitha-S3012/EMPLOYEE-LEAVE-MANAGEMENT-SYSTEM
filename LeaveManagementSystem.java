<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Leave Management App</title>
    <!-- Load Tailwind CSS -->
    <script src="https://cdn.tailwindcss.com"></script>
    <style>
        /* Using Inter font as a default */
        body {
            font-family: 'Inter', sans-serif;
        }
        /* Custom styles for disabled buttons */
        .btn-disabled {
            background-color: #d1d5db; /* gray-300 */
            cursor: not-allowed;
        }
    </style>
</head>
<body class="bg-gray-100 min-h-screen flex items-center justify-center p-4">

    <div class="bg-white w-full max-w-3xl rounded-xl shadow-xl p-8">
        
        <h1 class="text-3xl font-bold text-center text-gray-800 mb-8">Leave Management System</h1>

        <!-- Main Grid Layout -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-8">

            <!-- Left Column: Submit Request -->
            <div class="border-r-0 md:border-r md:pr-8 border-gray-200">
                <h2 class="text-2xl font-semibold text-gray-700 mb-4">Submit a Request</h2>
                
                <form id="leave-form" class="space-y-4">
                    <div>
                        <label for="employee-select" class="block text-sm font-medium text-gray-700">I am:</label>
                        <select id="employee-select" class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500">
                            <option value="Alice Smith">Alice Smith (Employee)</option>
                            <option value="Bob Johnson">Bob Johnson (Employee)</option>
                            <option value="Sarah Jenkins">Sarah Jenkins (Manager)</option>
                        </select>
                    </div>

                    <div>
                        <label for="leave-days" class="block text-sm font-medium text-gray-700">Number of Days</label>
                        <input type="number" id="leave-days" min="1" class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500" placeholder="e.g., 3" required>
                    </div>

                    <div>
                        <label for="leave-reason" class="block text-sm font-medium text-gray-700">Reason</label>
                        <textarea id="leave-reason" rows="3" class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500" placeholder="e.g., Family vacation" required></textarea>
                    </div>

                    <button type="submit" class="w-full bg-indigo-600 text-white font-semibold py-2 px-4 rounded-lg shadow-md hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 transition duration-200">
                        Submit Request
                    </button>
                </form>
            </div>

            <!-- Right Column: Manager View -->
            <div>
                <h2 class="text-2xl font-semibold text-gray-700 mb-4">Manager View (Pending)</h2>
                <div id="pending-list" class="space-y-3 h-48 overflow-y-auto">
                    <!-- Pending requests will be dynamically inserted here -->
                    <p id="no-pending-msg" class="text-gray-500">No pending requests.</p>
                </div>

                <h2 class="text-2xl font-semibold text-gray-700 mt-6 mb-4">Request History</h2>
                <div id="history-list" class="space-y-3 h-48 overflow-y-auto bg-gray-50 p-4 rounded-lg">
                    <!-- History of requests will be dynamically inserted here -->
                    <p id="no-history-msg" class="text-gray-500">No approved or rejected requests yet.</p>
                </div>
            </div>

        </div>
    </div>

    <script>
        document.addEventListener('DOMContentLoaded', () => {
            // --- STATE ---
            let leaveRequests = [];
            let requestIdCounter = 0;

            // --- DOM ELEMENTS ---
            const form = document.getElementById('leave-form');
            const employeeSelect = document.getElementById('employee-select');
            const daysInput = document.getElementById('leave-days');
            const reasonInput = document.getElementById('leave-reason');
            
            const pendingList = document.getElementById('pending-list');
            const historyList = document.getElementById('history-list');
            
            const noPendingMsg = document.getElementById('no-pending-msg');
            const noHistoryMsg = document.getElementById('no-history-msg');

            // --- FUNCTIONS ---

            /**
             * Renders all pending and historical requests.
             * This function is called every time the state changes.
             */
            function renderLists() {
                // Clear current lists
                pendingList.innerHTML = '';
                historyList.innerHTML = '';

                let hasPending = false;
                let hasHistory = false;

                // Sort requests to show newest first
                const sortedRequests = [...leaveRequests].reverse();

                for (const request of sortedRequests) {
                    if (request.status === 'Pending') {
                        hasPending = true;
                        pendingList.appendChild(createPendingRequestElement(request));
                    } else {
                        hasHistory = true;
                        historyList.appendChild(createHistoryRequestElement(request));
                    }
                }

                // Show/hide placeholder messages
                noPendingMsg.style.display = hasPending ? 'none' : 'block';
                noHistoryMsg.style.display = hasHistory ? 'none' : 'block';
            }

            /**
             * Creates an HTML element for a pending request.
             */
            function createPendingRequestElement(request) {
                const item = document.createElement('div');
                item.className = 'p-3 bg-white border border-gray-200 rounded-lg shadow-sm';
                
                item.innerHTML = `
                    <p class="font-semibold text-gray-800">${request.employeeName}</p>
                    <p class="text-sm text-gray-600">${request.days} days - ${request.reason}</p>
                    <div class="mt-2 space-x-2">
                        <button data-id="${request.id}" class="approve-btn text-xs font-medium bg-green-500 text-white py-1 px-3 rounded-md hover:bg-green-600 transition">
                            Approve
                        </button>
                        <button data-id="${request.id}" class="reject-btn text-xs font-medium bg-red-500 text-white py-1 px-3 rounded-md hover:bg-red-600 transition">
                            Reject
                        </button>
                    </div>
                `;
                return item;
            }

            /**
             * Creates an HTML element for a historical (approved/rejected) request.
             */
            function createHistoryRequestElement(request) {
                const item = document.createElement('div');
                item.className = 'p-3 bg-white border border-gray-200 rounded-lg';
                
                const statusClass = request.status === 'Approved' ? 'text-green-600' : 'text-red-600';
                
                item.innerHTML = `
                    <p class="font-semibold text-gray-800">${request.employeeName}</p>
                    <p class="text-sm text-gray-600">${request.days} days - ${request.reason}</p>
                    <p class="text-sm font-bold ${statusClass}">${request.status}</p>
                `;
                return item;
            }

            /**
             * Handles the form submission to create a new leave request.
             */
            function handleSubmit(e) {
                e.preventDefault();

                const employeeName = employeeSelect.value;
                const days = daysInput.value;
                const reason = reasonInput.value;

                if (!days || !reason) {
                    // Simple validation
                    return;
                }

                // Create new request object
                const newRequest = {
                    id: requestIdCounter++,
                    employeeName: employeeName,
                    days: parseInt(days),
                    reason: reason,
                    status: 'Pending'
                };

                // Add to state
                leaveRequests.push(newRequest);

                // Re-render
                renderLists();

                // Reset form
                form.reset();
            }

            /**
             * Handles clicks on the "Approve" or "Reject" buttons.
             */
            function handleManagerAction(e) {
                const target = e.target;
                const requestId = parseInt(target.getAttribute('data-id'));

                // Find the request
                const request = leaveRequests.find(r => r.id === requestId);
                if (!request) return;

                if (target.classList.contains('approve-btn')) {
                    // Approve action
                    request.status = 'Approved';
                } else if (target.classList.contains('reject-btn')) {
                    // Reject action
                    request.status = 'Rejected';
                }

                // Re-render all lists
                renderLists();
            }

            // --- EVENT LISTENERS ---
            form.addEventListener('submit', handleSubmit);
            
            // Listen for clicks on the parent lists to handle dynamic buttons
            pendingList.addEventListener('click', handleManagerAction);

            // --- INITIAL RENDER ---
            renderLists();
        });
    </script>
</body>
</html>
